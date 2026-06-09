package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.remote.NetworkClients
import com.example.data.repository.*
import com.example.util.SecurityUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    
    val configRepo = SchoolConfigRepository(db.schoolConfigDao())
    val mealRepo = MealRepository(db.mealDao())
    val timetableRepo = TimetableRepository(db.timetableDao())
    val eventRepo = SchoolEventRepository(db.schoolEventDao())
    val customEventRepo = CustomEventRepository(db.customEventDao())
    val memoRepo = TimetableMemoRepository(db.timetableMemoDao())

    // --- State Observables ---
    val config: StateFlow<SchoolConfig?> = configRepo.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedDate = MutableStateFlow(getTodayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    // School search state in Settings
    private val _schoolSearchResults = MutableStateFlow<List<SchoolConfig>>(emptyList())
    val schoolSearchResults: StateFlow<List<SchoolConfig>> = _schoolSearchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Refreshing loader
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Enrichment AI Loader
    private val _enrichmentLoading = MutableStateFlow(false)
    val enrichmentLoading: StateFlow<Boolean> = _enrichmentLoading.asStateFlow()

    private val _enrichedMeal = MutableStateFlow<MealEntity?>(null)
    val enrichedMeal: StateFlow<MealEntity?> = _enrichedMeal.asStateFlow()

    private val _hasCustomNeisKey = MutableStateFlow(false)
    val hasCustomNeisKey: StateFlow<Boolean> = _hasCustomNeisKey.asStateFlow()

    init {
        // Initialize security utilities on start
        SecurityUtil.initialize(application)
        _hasCustomNeisKey.value = SecurityUtil.hasCustomNeisKey(application)

        // Ensure default configuration exists so config Flow emits
        viewModelScope.launch {
            if (configRepo.getDirectConfig() == null) {
                configRepo.saveConfig(SchoolConfig())
            }
        }
        
        // Auto-refresh when config or selectedDate changes
        viewModelScope.launch {
            combine(config, _selectedDate) { cfg, date -> Pair(cfg, date) }
                .collect { (cfg, date) ->
                    if (cfg != null && cfg.sdSchulCode.isNotEmpty()) {
                        preheatDataForMonth(cfg, date)
                    }
                }
        }

        // Auto-sync timetable to system calendar when timetable or config changes
        viewModelScope.launch {
            combine(monthlyTimetable, config) { timetable, cfg -> Pair(timetable, cfg) }
                .collect { (timetable, cfg) ->
                    if (cfg != null && cfg.sdSchulCode.isNotEmpty()) {
                        syncTimetableToSystemCalendar(timetable, cfg)
                    }
                }
        }
    }

    // --- Meal Data Stream ---
    val mealsForSelectedDate: StateFlow<List<MealEntity>> = _selectedDate
        .flatMapLatest { date -> mealRepo.getMealsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Timetable Stream ---
    val timetableForSelectedDate: StateFlow<List<TimetableEntity>> = _selectedDate
        .flatMapLatest { date -> timetableRepo.getTimetableForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Events & Custom Events Stream ---
    val schoolEventsForSelectedDate: StateFlow<List<SchoolEventEntity>> = _selectedDate
        .flatMapLatest { date ->
            // Academic schedules often are fetched in calendar range, but to show on selected date:
            eventRepo.getEventsForRange(date, date)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customEventsForSelectedDate: StateFlow<List<CustomEventEntity>> = _selectedDate
        .flatMapLatest { date -> customEventRepo.getCustomEventsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Calendar Markers for Monthly View ---
    // Let's expose monthly schedules to draw markers on TableCalendar
    private val _currentMonthRange = MutableStateFlow(getMonthRangeForDate(getTodayString()))
    
    val monthlySchoolEvents: StateFlow<List<SchoolEventEntity>> = _currentMonthRange
        .flatMapLatest { range -> eventRepo.getEventsForRange(range.first, range.second) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyCustomEvents: StateFlow<List<CustomEventEntity>> = _currentMonthRange
        .flatMapLatest { range -> customEventRepo.getCustomEventsForRange(range.first, range.second) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Timetable Memos & Monthly Streams ---
    val memosForSelectedDate: StateFlow<List<TimetableMemoEntity>> = _selectedDate
        .flatMapLatest { date -> memoRepo.getMemosForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyMemos: StateFlow<List<TimetableMemoEntity>> = _currentMonthRange
        .flatMapLatest { range -> memoRepo.getMemosForRange(range.first, range.second) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTimetable: StateFlow<List<TimetableEntity>> = _currentMonthRange
        .flatMapLatest { range -> timetableRepo.getTimetableForRange(range.first, range.second) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Action Methods ---

    fun saveNeisApiKey(key: String) {
        SecurityUtil.saveCustomNeisKey(getApplication(), key)
        _hasCustomNeisKey.value = SecurityUtil.hasCustomNeisKey(getApplication())
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        _currentMonthRange.value = getMonthRangeForDate(date)
    }

    fun completeOnboarding(school: SchoolConfig, grade: String, classNm: String) {
        viewModelScope.launch {
            val configWithRows = school.copy(
                id = 1,
                grade = grade,
                classNm = classNm,
                isFirstLaunch = false
            )
            configRepo.saveConfig(configWithRows)
        }
    }

    fun saveSchoolSettings(schoolName: String, officeCode: String, schoolCode: String, kind: String, grade: String, classNm: String) {
        viewModelScope.launch {
            val current = config.value ?: SchoolConfig()
            val updated = current.copy(
                schulNm = schoolName,
                atptOfcdcScCode = officeCode,
                sdSchulCode = schoolCode,
                schoolKind = kind,
                grade = grade,
                classNm = classNm
            )
            configRepo.saveConfig(updated)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            configRepo.updateThemeMode(mode)
        }
    }

    fun searchSchools(query: String) {
        searchJob?.cancel()
        if (query.isEmpty()) {
            _schoolSearchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            try {
                kotlinx.coroutines.delay(300)
                val results = NetworkClients.searchSchool(getApplication(), query)
                _schoolSearchResults.value = results
            } catch (e: Exception) {
                Log.e("VM_SEARCH", "School search failed", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addCustomEvent(title: String, time: String, syncToSystem: Boolean) {
        viewModelScope.launch {
            customEventRepo.addCustomEvent(
                context = getApplication(),
                date = _selectedDate.value,
                title = title,
                time = time,
                syncToSystem = syncToSystem
            )
        }
    }

    fun deleteCustomEvent(id: Int) {
        viewModelScope.launch {
            customEventRepo.deleteCustomEvent(getApplication(), id)
        }
    }

    fun saveTimetableMemo(date: String, perio: Int, memo: String) {
        viewModelScope.launch {
            memoRepo.saveMemo(date, perio, memo)
        }
    }

    private fun syncTimetableToSystemCalendar(timetable: List<TimetableEntity>, cfg: SchoolConfig) {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            viewModelScope.launch(Dispatchers.IO) {
                timetableRepo.syncToSystemCalendar(context, cfg.schoolKind, timetable)
            }
        }
    }

    fun triggerTimetableSync() {
        val cfg = config.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val range = _currentMonthRange.value
            val timetable = timetableRepo.getTimetableForRangeDirect(range.first, range.second)
            syncTimetableToSystemCalendar(timetable, cfg)
        }
    }

    /**
     * Enriches a meal with Gemini description and Scraped DuckDuckGo image.
     */
    fun enrichMealDetails(mealId: String) {
        viewModelScope.launch {
            _enrichmentLoading.value = true
            _enrichedMeal.value = null
            try {
                val enriched = mealRepo.getOrGenerateMealAnalysisAndImage(getApplication(), mealId)
                _enrichedMeal.value = enriched
            } catch (e: Exception) {
                Log.e("VM_ENRICH", "Failed to enrich food details", e)
            } finally {
                _enrichmentLoading.value = false
            }
        }
    }

    /**
     * Pre-heats/Fetches calendar data for the entire selected month range
     * to fulfill the "Offline Caching Strategy".
     */
    fun refreshCurrentData() {
        val cfg = config.value ?: return
        if (cfg.sdSchulCode.isEmpty()) return
        
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val range = _currentMonthRange.value
                val isOnline = isNetworkConnected()
                
                // Refresh meals
                mealRepo.refreshMeals(
                    context = getApplication(),
                    officeCode = cfg.atptOfcdcScCode,
                    schoolCode = cfg.sdSchulCode,
                    fromDate = range.first,
                    toDate = range.second,
                    isOnline = isOnline
                )
                
                // Refresh timetable
                timetableRepo.refreshTimetable(
                    context = getApplication(),
                    officeCode = cfg.atptOfcdcScCode,
                    schoolCode = cfg.sdSchulCode,
                    schoolKind = cfg.schoolKind,
                    grade = cfg.grade,
                    classNm = cfg.classNm,
                    fromDate = range.first,
                    toDate = range.second,
                    isOnline = isOnline
                )
                
                // Refresh school academic events
                eventRepo.refreshEvents(
                    context = getApplication(),
                    officeCode = cfg.atptOfcdcScCode,
                    schoolCode = cfg.sdSchulCode,
                    fromDate = range.first,
                    toDate = range.second,
                    isOnline = isOnline
                )
            } catch (e: Exception) {
                Log.e("VM_REFRESH", "Data range refresh failed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun preheatDataForMonth(cfg: SchoolConfig, date: String) {
        val range = getMonthRangeForDate(date)
        val isOnline = isNetworkConnected()
        
        // Silently fetch meals, timetable and calendar events in background
        try {
            mealRepo.refreshMeals(getApplication(), cfg.atptOfcdcScCode, cfg.sdSchulCode, range.first, range.second, isOnline)
            timetableRepo.refreshTimetable(getApplication(), cfg.atptOfcdcScCode, cfg.sdSchulCode, cfg.schoolKind, cfg.grade, cfg.classNm, range.first, range.second, isOnline)
            eventRepo.refreshEvents(getApplication(), cfg.atptOfcdcScCode, cfg.sdSchulCode, range.first, range.second, isOnline)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Helper Utility Methods ---

    private fun isNetworkConnected(): Boolean {
        return true
    }

    fun getTodayString(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    }

    private fun getMonthRangeForDate(dateStr: String): Pair<String, String> {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val date = try { sdf.parse(dateStr) } catch(e: Exception) { Date() } ?: Date()
        
        val cal = Calendar.getInstance()
        cal.time = date
        
        // Start of month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = sdf.format(cal.time)
        
        // End of month
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = sdf.format(cal.time)
        
        return Pair(start, end)
    }
}
