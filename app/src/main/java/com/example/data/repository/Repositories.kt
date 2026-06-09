package com.example.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.example.data.local.*
import com.example.data.remote.NetworkClients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class SchoolConfigRepository(private val dao: SchoolConfigDao) {
    val config: Flow<SchoolConfig?> = dao.getConfig()

    suspend fun getDirectConfig(): SchoolConfig? = withContext(Dispatchers.IO) {
        dao.getConfigDirect()
    }

    suspend fun saveConfig(config: SchoolConfig) = withContext(Dispatchers.IO) {
        dao.insertConfig(config)
    }

    suspend fun setFirstLaunchCompleted() = withContext(Dispatchers.IO) {
        dao.updateFirstLaunch(false)
    }

    suspend fun updateThemeMode(mode: String) = withContext(Dispatchers.IO) {
        val current = dao.getConfigDirect() ?: SchoolConfig()
        dao.insertConfig(current.copy(themeMode = mode))
    }
}

class MealRepository(private val dao: MealDao) {
    
    fun getMealsForDate(date: String): Flow<List<MealEntity>> = dao.getMealsForDate(date)

    fun getMealsForRange(startDate: String, endDate: String): Flow<List<MealEntity>> {
        return dao.getMealsForRange(startDate, endDate)
    }

    /**
     * Freshly fetches meals from NEIS is connected, otherwise reads from Room.
     */
    suspend fun refreshMeals(
        context: Context,
        officeCode: String,
        schoolCode: String,
        fromDate: String,
        toDate: String,
        isOnline: Boolean
    ) = withContext(Dispatchers.IO) {
        if (isOnline) {
            val freshMeals = NetworkClients.fetchMeals(context, officeCode, schoolCode, fromDate, toDate)
            if (freshMeals.isNotEmpty()) {
                // Keep any preexisting AI descriptions and images!
                val cachedMeals = mutableListOf<MealEntity>()
                for (fresh in freshMeals) {
                    val existing = dao.getMealById(fresh.id)
                    if (existing != null) {
                        cachedMeals.add(
                            fresh.copy(
                                aiDescription = existing.aiDescription,
                                imageUrl = existing.imageUrl
                            )
                        )
                    } else {
                        cachedMeals.add(fresh)
                    }
                }
                dao.insertMeals(cachedMeals)
            }
        }
    }

    /**
     * Smart caching: if and only if aiDescription and imageUrl do not exist, fetch them and cache!
     */
    suspend fun getOrGenerateMealAnalysisAndImage(context: Context, mealId: String): MealEntity? = withContext(Dispatchers.IO) {
        val meal = dao.getMealById(mealId) ?: return@withContext null
        
        val needsAi = meal.aiDescription.isNullOrEmpty()
        val needsImg = meal.imageUrl.isNullOrEmpty()
        
        if (!needsAi && !needsImg) {
            return@withContext meal // Read entirely from cached DB
        }
        
        var aiDesc = meal.aiDescription
        var imgUrl = meal.imageUrl
        
        // Pick primary dish name (usually first or second in string) for DDG image search
        val primaryDish = meal.dishes.split(", ").firstOrNull() ?: meal.mealType
        
        if (needsAi) {
            aiDesc = NetworkClients.fetchGeminiMealAnalysis(context, meal.dishes)
        }
        
        if (needsImg) {
            imgUrl = NetworkClients.searchDdgImage(primaryDish)
        }
        
        val updatedMeal = meal.copy(
            aiDescription = aiDesc,
            imageUrl = imgUrl
        )
        dao.insertMeal(updatedMeal)
        return@withContext updatedMeal
    }
}

class TimetableRepository(private val dao: TimetableDao) {
    fun getTimetableForDate(date: String): Flow<List<TimetableEntity>> = dao.getTimetableForDate(date)

    fun getTimetableForRange(startDate: String, endDate: String): Flow<List<TimetableEntity>> {
        return dao.getTimetableForRange(startDate, endDate)
    }

    suspend fun getTimetableForRangeDirect(startDate: String, endDate: String): List<TimetableEntity> = withContext(Dispatchers.IO) {
        dao.getTimetableForRangeDirect(startDate, endDate)
    }

    suspend fun syncToSystemCalendar(
        context: Context,
        schoolKind: String,
        timetableList: List<TimetableEntity>
    ) = withContext(Dispatchers.IO) {
        try {
            // Delete previously synced timetable entries
            val selection = "${CalendarContract.Events.DESCRIPTION} = ?"
            val selectionArgs = arrayOf("SmartSchoolLifeTimetableSync")
            context.contentResolver.delete(CalendarContract.Events.CONTENT_URI, selection, selectionArgs)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        for (item in timetableList) {
            val periodTimes = getPeriodTimes(schoolKind, item.perio)
            if (item.date.length < 8) continue
            val year = item.date.substring(0, 4).toIntOrNull() ?: continue
            val month = (item.date.substring(4, 6).toIntOrNull() ?: 1) - 1
            val day = item.date.substring(6, 8).toIntOrNull() ?: continue

            val startCal = Calendar.getInstance().apply {
                set(year, month, day, periodTimes.first.first, periodTimes.first.second)
            }
            val endCal = Calendar.getInstance().apply {
                set(year, month, day, periodTimes.second.first, periodTimes.second.second)
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startCal.timeInMillis)
                put(CalendarContract.Events.DTEND, endCal.timeInMillis)
                put(CalendarContract.Events.TITLE, "[시간표] ${item.subject}")
                put(CalendarContract.Events.DESCRIPTION, "SmartSchoolLifeTimetableSync")
                put(CalendarContract.Events.CALENDAR_ID, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            try {
                context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getPeriodTimes(schoolKind: String, perio: Int): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        return when {
            schoolKind.contains("고등학교") -> {
                when (perio) {
                    1 -> Pair(Pair(9, 0), Pair(9, 50))
                    2 -> Pair(Pair(10, 0), Pair(10, 50))
                    3 -> Pair(Pair(11, 0), Pair(11, 50))
                    4 -> Pair(Pair(12, 0), Pair(12, 50))
                    5 -> Pair(Pair(13, 50), Pair(14, 40))
                    6 -> Pair(Pair(14, 50), Pair(15, 40))
                    else -> Pair(Pair(15, 50), Pair(16, 40))
                }
            }
            schoolKind.contains("중학교") -> {
                when (perio) {
                    1 -> Pair(Pair(9, 0), Pair(9, 45))
                    2 -> Pair(Pair(9, 55), Pair(10, 40))
                    3 -> Pair(Pair(10, 50), Pair(11, 35))
                    4 -> Pair(Pair(11, 45), Pair(12, 30))
                    5 -> Pair(Pair(13, 30), Pair(14, 15))
                    6 -> Pair(Pair(14, 25), Pair(15, 10))
                    else -> Pair(Pair(15, 20), Pair(16, 5))
                }
            }
            else -> { // 초등학교
                when (perio) {
                    1 -> Pair(Pair(9, 0), Pair(9, 40))
                    2 -> Pair(Pair(9, 50), Pair(10, 30))
                    3 -> Pair(Pair(10, 40), Pair(11, 20))
                    4 -> Pair(Pair(11, 30), Pair(12, 10))
                    5 -> Pair(Pair(13, 0), Pair(13, 40))
                    else -> Pair(Pair(13, 50), Pair(14, 30))
                }
            }
        }
    }

    suspend fun refreshTimetable(
        context: Context,
        officeCode: String,
        schoolCode: String,
        schoolKind: String,
        grade: String,
        classNm: String,
        fromDate: String,
        toDate: String,
        isOnline: Boolean
    ) = withContext(Dispatchers.IO) {
        if (isOnline) {
            val list = NetworkClients.fetchTimetable(
                context, officeCode, schoolCode, schoolKind, grade, classNm, fromDate, toDate
            )
            if (list.isNotEmpty()) {
                dao.deleteTimetableForRange(fromDate, toDate)
                dao.insertTimetable(list)
            }
        }
    }
}

class SchoolEventRepository(private val dao: SchoolEventDao) {
    fun getEventsForRange(startDate: String, endDate: String): Flow<List<SchoolEventEntity>> {
        return dao.getEventsForRange(startDate, endDate)
    }

    suspend fun refreshEvents(
        context: Context,
        officeCode: String,
        schoolCode: String,
        fromDate: String,
        toDate: String,
        isOnline: Boolean
    ) = withContext(Dispatchers.IO) {
        if (isOnline) {
            val list = NetworkClients.fetchSchoolEvents(context, officeCode, schoolCode, fromDate, toDate)
            if (list.isNotEmpty()) {
                dao.deleteEventsForRange(fromDate, toDate)
                dao.insertEvents(list)
            }
        }
    }
}

class CustomEventRepository(private val dao: CustomEventDao) {
    fun getCustomEventsForDate(date: String): Flow<List<CustomEventEntity>> = dao.getCustomEventsForDate(date)
    
    fun getCustomEventsForRange(startDate: String, endDate: String): Flow<List<CustomEventEntity>> {
        return dao.getCustomEventsForRange(startDate, endDate)
    }

    suspend fun addCustomEvent(
        context: Context,
        date: String,
        title: String,
        time: String,
        syncToSystem: Boolean
    ) = withContext(Dispatchers.IO) {
        var systemEventId: Long? = null
        if (syncToSystem) {
            systemEventId = syncToAndroidSystemCalendar(context, title, date, time)
        }
        
        val event = CustomEventEntity(
            date = date,
            title = title,
            time = time,
            syncedToSystem = syncToSystem && systemEventId != null,
            systemEventId = systemEventId
        )
        dao.insertCustomEvent(event)
    }

    suspend fun deleteCustomEvent(context: Context, id: Int) = withContext(Dispatchers.IO) {
        val event = dao.getCustomEventById(id)
        if (event != null) {
            if (event.syncedToSystem && event.systemEventId != null) {
                try {
                    val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.systemEventId)
                    context.contentResolver.delete(deleteUri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            dao.deleteCustomEvent(id)
        }
    }

    private fun syncToAndroidSystemCalendar(
        context: Context,
        title: String,
        dateStr: String,
        timeStr: String
    ): Long? {
        return try {
            val year = dateStr.substring(0, 4).toInt()
            val month = dateStr.substring(4, 6).toInt() - 1 // Calendar is 0-indexed
            val day = dateStr.substring(6, 8).toInt()
            
            var hour = 9
            var minute = 0
            if (timeStr.isNotEmpty() && timeStr.contains(":")) {
                val parts = timeStr.split(":")
                hour = parts[0].trim().toIntOrNull() ?: 9
                minute = parts[1].trim().toIntOrNull() ?: 0
            }

            val cal = Calendar.getInstance()
            cal.set(year, month, day, hour, minute)
            val startMillis = cal.timeInMillis
            val endMillis = startMillis + (60 * 60 * 1000) // Default 1 hour

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, "Smart School Life Calendar Event")
                // Calendar ID = 1 is the primary calendar on stock Android
                put(CalendarContract.Events.CALENDAR_ID, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            Log.e("SYSTEM_CALENDAR", "Failed to sync event to Android System Calendar", e)
            null
        }
    }
}

class TimetableMemoRepository(private val dao: TimetableMemoDao) {
    fun getMemosForDate(date: String): Flow<List<TimetableMemoEntity>> = dao.getMemosForDate(date)
    fun getMemosForRange(startDate: String, endDate: String): Flow<List<TimetableMemoEntity>> = dao.getMemosForRange(startDate, endDate)
    suspend fun saveMemo(date: String, perio: Int, memoText: String) = withContext(Dispatchers.IO) {
        val id = "${date}_$perio"
        if (memoText.trim().isEmpty()) {
            dao.deleteMemo(id)
        } else {
            dao.insertMemo(TimetableMemoEntity(id, date, perio, memoText.trim()))
        }
    }
}
