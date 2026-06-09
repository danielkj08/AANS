package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.TimeExtractor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: MainViewModel) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Meals, 1: Timetable, 2: Calendar
    var showSettings by remember { mutableStateOf(false) }

    if (config == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentConfig = config!!

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 4.dp)
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = currentConfig.schulNm,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentConfig.grade}학년 ${currentConfig.classNm}반",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AES-256 Vault Active",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshCurrentData() },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Data", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = null) },
                        label = { Text("오늘의 급식", fontSize = 11.sp, fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium) },
                        modifier = Modifier.testTag("nav_tab_meals"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        label = { Text("시간표", fontSize = 11.sp, fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium) },
                        modifier = Modifier.testTag("nav_tab_timetable"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        label = { Text("학업 캘린더", fontSize = 11.sp, fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Medium) },
                        modifier = Modifier.testTag("nav_tab_calendar"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> MealsTab(viewModel = viewModel, selectedDate = selectedDate, config = currentConfig)
                1 -> TimetableTab(viewModel = viewModel, selectedDate = selectedDate, config = currentConfig)
                2 -> CalendarTab(viewModel = viewModel, selectedDate = selectedDate)
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            config = currentConfig,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
fun TodayScheduleCard(
    schoolName: String,
    grade: String,
    classNm: String,
    selectedDate: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S SCHEDULE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NEIS SYNCED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Text(
                text = formatPrettifiedDate(selectedDate),
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = "$schoolName • ${grade}학년 ${classNm}반",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

// ==========================================
// 1. Meals Tab Component (급식)
// ==========================================
@Composable
fun MealsTab(
    viewModel: MainViewModel,
    selectedDate: String,
    config: SchoolConfig
) {
    val meals by viewModel.mealsForSelectedDate.collectAsState()
    val enrichmentLoading by viewModel.enrichmentLoading.collectAsState()
    val enrichedMeal by viewModel.enrichedMeal.collectAsState()
    
    var selectedMealForDetail by remember { mutableStateOf<MealEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TodayScheduleCard(
            schoolName = config.schulNm,
            grade = config.grade,
            classNm = config.classNm,
            selectedDate = selectedDate
        )
        
        Box(modifier = Modifier.weight(1f)) {
            if (meals.isEmpty()) {
                EmptyListPlaceholder(
                    title = "오늘 급식 일정이 없습니다",
                    subtitle = "주말, 공휴일이거나 아직 불러온 데이터가 없습니다.\n상단의 새로고침 기호를 눌러 NEIS 서버에서 최신 급식을 받아올 수 있습니다.",
                    icon = Icons.Default.Restaurant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(meals) { meal ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMealForDetail = meal
                                    viewModel.enrichMealDetails(meal.id)
                                }
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                                .testTag("meal_item_${meal.mealType}"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.RestaurantMenu,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = meal.mealType,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    Text(
                                        text = meal.calories,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = meal.dishes,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "AI 분석 & 실물 사진 보기",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Expanded detail drawer with Gemini and DDG Images
            if (selectedMealForDetail != null) {
                AlertDialog(
                    onDismissRequest = { selectedMealForDetail = null },
                    confirmButton = {
                        TextButton(onClick = { selectedMealForDetail = null }) {
                            Text("닫기", fontWeight = FontWeight.Bold)
                        }
                    },
                    title = {
                        Text(
                            text = "${selectedMealForDetail!!.mealType} 식단 및 AI 리포트",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (enrichmentLoading) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Gemini 영양사와 통신 중...\n푸드 사진을 검색하는 중입니다 🍳",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                val activeMeal = enrichedMeal ?: selectedMealForDetail!!
                                
                                // 1. Food Image
                                if (!activeMeal.imageUrl.isNullOrEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        AsyncImage(
                                            model = activeMeal.imageUrl,
                                            contentDescription = "Food Scraped Dish",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                
                                // 2. Dishes
                                Text(
                                    text = "📝 식단 요약\n${activeMeal.dishes}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                // 3. Gemini Report (Professional Polish Colors mapped exactly)
                                if (!activeMeal.aiDescription.isNullOrEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFFD0BCFF), RoundedCornerShape(16.dp)),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFEF7FF)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Psychology,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6750A4),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "급식 영양사의 식습관 코치",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF6750A4),
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = activeMeal.aiDescription!!,
                                                fontSize = 13.sp,
                                                lineHeight = 20.sp,
                                                color = Color(0xFF49454F)
                                            )
                                        }
                                    }
                                }

                                // 4. Origins & Nutrition Matrix
                                if (activeMeal.nutrition.isNotEmpty()) {
                                    AccordionSection(title = "🍎 영양 성분표") {
                                        Text(activeMeal.nutrition, fontSize = 11.sp, lineHeight = 16.sp)
                                    }
                                }
                                if (activeMeal.origins.isNotEmpty()) {
                                    AccordionSection(title = "🌾 원산지 정보") {
                                        Text(activeMeal.origins, fontSize = 11.sp, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AccordionSection(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

// ==========================================
// 2. Timetable Tab Component (시간표)
// ==========================================
@Composable
fun TimetableTab(
    viewModel: MainViewModel,
    selectedDate: String,
    config: SchoolConfig
) {
    val classes by viewModel.timetableForSelectedDate.collectAsState()
    val memos by viewModel.memosForSelectedDate.collectAsState()
    var editingPeriodMemo by remember { mutableStateOf<TimetableEntity?>(null) }
    
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val writeGranted = perms[Manifest.permission.WRITE_CALENDAR] == true
        if (writeGranted) {
            viewModel.triggerTimetableSync()
        }
    }

    LaunchedEffect(Unit) {
        val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (!hasWrite) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)
            )
        } else {
            viewModel.triggerTimetableSync()
        }
    }

    val fullClasses = remember(classes, selectedDate) {
        if (classes.isEmpty()) emptyList()
        else {
            val maxPeriod = maxOf(7, classes.maxOfOrNull { it.perio } ?: 7)
            (1..maxPeriod).flatMap { p ->
                val matching = classes.filter { it.perio == p }
                if (matching.isEmpty()) {
                    listOf(
                        TimetableEntity(
                            id = "empty_$p",
                            date = selectedDate,
                            perio = p,
                            subject = "수업 없음"
                        )
                    )
                } else {
                    matching
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TodayScheduleCard(
            schoolName = config.schulNm,
            grade = config.grade,
            classNm = config.classNm,
            selectedDate = selectedDate
        )
        
        Text(
            text = "오늘의 수업 일정 (수업을 눌러 메모 작성)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Box(modifier = Modifier.weight(1f)) {
            if (fullClasses.isEmpty()) {
                EmptyListPlaceholder(
                    title = "등록된 수업 시간표가 없습니다",
                    subtitle = "오늘은 수업이 없는 날이거나 시간표를 받지 못했습니다.\n재검색하거나 상단의 새로고침 기호로 수업 목록을 갱신해 보세요.",
                    icon = Icons.Default.MenuBook
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fullClasses) { period ->
                        val isEmpty = period.subject == "수업 없음"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isEmpty) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isEmpty) 0.15f else 0.35f), RoundedCornerShape(16.dp))
                                .clickable {
                                    if (!isEmpty) {
                                        editingPeriodMemo = period
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isEmpty) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${period.perio}",
                                    color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = period.subject,
                                    fontSize = 16.sp,
                                    fontWeight = if (isEmpty) FontWeight.Medium else FontWeight.SemiBold,
                                    color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                                )
                                val memo = memos.find { it.perio == period.perio }
                                if (memo != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📝 ${memo.memo}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "${period.perio}교시",
                                fontSize = 11.sp,
                                color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingPeriodMemo != null) {
        val period = editingPeriodMemo!!
        val existingMemo = memos.find { it.perio == period.perio }?.memo ?: ""
        var memoText by remember { mutableStateOf(existingMemo) }
        
        AlertDialog(
            onDismissRequest = { editingPeriodMemo = null },
            title = { Text(text = "${period.perio}교시 [${period.subject}] 메모", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    label = { Text("수업 메모 입력") },
                    placeholder = { Text("준비물, 숙제, 학습 내용 등...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveTimetableMemo(selectedDate, period.perio, memoText)
                        editingPeriodMemo = null
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPeriodMemo = null }) {
                    Text("취소")
                }
            }
        )
    }
}

// ==========================================
// 3. Calendar Tab Component (학사 및 스마트 개인 일정)
// ==========================================
@Composable
fun CalendarTab(
    viewModel: MainViewModel,
    selectedDate: String
) {
    val schoolEvents by viewModel.schoolEventsForSelectedDate.collectAsState()
    val customEvents by viewModel.customEventsForSelectedDate.collectAsState()
    val memos by viewModel.memosForSelectedDate.collectAsState()
    
    val monthlySchEvents by viewModel.monthlySchoolEvents.collectAsState()
    val monthlyCustEvents by viewModel.monthlyCustomEvents.collectAsState()
    val monthlyMemos by viewModel.monthlyMemos.collectAsState()

    var showAddEventDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // High fidelity custom M3 calendar view
        CustomCalendarView(
            selectedDate = selectedDate,
            monthlySchoolEvents = monthlySchEvents,
            monthlyCustomEvents = monthlyCustEvents,
            monthlyMemos = monthlyMemos,
            onDaySelected = { day ->
                viewModel.selectDate(day)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatPrettifiedDate(selectedDate)} 일정 정보",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Button(
                onClick = { showAddEventDialog = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.testTag("add_event_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("일정 추가", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (schoolEvents.isEmpty() && customEvents.isEmpty() && memos.isEmpty()) {
            EmptyListPlaceholder(
                title = "예정된 학사 및 개인 일정이 없습니다",
                subtitle = "오늘은 평화로운 날이네요! 새로운 학우 미팅, 수행평가 기간 등의 할 일을 우측 일정 추가로 스마트하게 기록해두세요.",
                icon = Icons.Default.EventAvailable
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Official school events
                items(schoolEvents) { ev ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = "School Event",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "[공식 학사] ${ev.eventName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (ev.eventContent.isNotEmpty()) {
                                Text(
                                    text = ev.eventContent,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Custom events
                items(customEvents) { ev ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = "Personal Event",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ev.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (ev.time.isNotEmpty()) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = ev.time,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (ev.syncedToSystem) {
                                    if (ev.time.isNotEmpty()) Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "시스템 동기화 완료",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        
                        IconButton(
                            onClick = { viewModel.deleteCustomEvent(ev.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Event",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Timetable class memos
                items(memos) { memo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = "Class Memo",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "[수업 메모] ${memo.perio}교시",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = memo.memo,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.saveTimetableMemo(selectedDate, memo.perio, "") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Memo",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        AddEventDialog(
            onDismiss = { showAddEventDialog = false },
            onSave = { title, time, sync ->
                viewModel.addCustomEvent(title, time, sync)
                showAddEventDialog = false
            }
        )
    }
}

// ------------------------------------------
// Multi-dot Highlight Calendar Component
// ------------------------------------------
@Composable
fun CustomCalendarView(
    selectedDate: String,
    monthlySchoolEvents: List<SchoolEventEntity>,
    monthlyCustomEvents: List<CustomEventEntity>,
    monthlyMemos: List<TimetableMemoEntity>,
    onDaySelected: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val monthSdf = SimpleDateFormat("yyyy년 M월", Locale.getDefault())
    
    // Maintain active calendar state
    var currentCalendar by remember { mutableStateOf(Calendar.getInstance().apply { time = try { sdf.parse(selectedDate) } catch(e: Exception) { Date() } ?: Date() }) }
    
    val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")
 
    // Calculations of grid dates
    val year = currentCalendar.get(Calendar.YEAR)
    val month = currentCalendar.get(Calendar.MONTH) // 0-indexed
    
    val gridDates = remember(year, month) {
        val dates = mutableListOf<String>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1: Sun, 2: Mon...
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Fill offsets before 1st of month
        for (i in 1 until firstDayOfWeek) {
            dates.add("") // empty placeholder
        }
        
        // Fill dates
        for (day in 1..daysInMonth) {
            val dateStr = String.format("%04d%02d%02d", year, month + 1, day)
            dates.add(dateStr)
        }
        
        // Ensure exactly 42 elements (6 rows * 7 days) to prevent calendar size jumping
        while (dates.size < 42) {
            dates.add("")
        }
        dates
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Calendar month controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            time = currentCalendar.time
                            add(Calendar.MONTH, -1)
                        }
                        currentCalendar = cal
                        onDaySelected(String.format("%04d%02d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, 1))
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month")
                }
                
                Text(
                    text = monthSdf.format(currentCalendar.time),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            time = currentCalendar.time
                            add(Calendar.MONTH, 1)
                        }
                        currentCalendar = cal
                        onDaySelected(String.format("%04d%02d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, 1))
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Week days headers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                weekDays.forEach { dayName ->
                    Text(
                        text = dayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dayName == "일") Color.Red else if (dayName == "토") Color.Blue else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Days grid using Column of Rows to dynamically fit height without truncation
            val rowsCount = 6
            Column(modifier = Modifier.fillMaxWidth()) {
                for (rowIndex in 0 until rowsCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (colIndex in 0 until 7) {
                            val index = rowIndex * 7 + colIndex
                            if (index < gridDates.size) {
                                val dateStr = gridDates[index]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dateStr.isNotEmpty()) {
                                        val dayNum = dateStr.substring(6, 8).toInt().toString()
                                        val isSelected = dateStr == selectedDate
                                        
                                        val hasSchoolEvent = monthlySchoolEvents.any { it.date == dateStr }
                                        val hasCustomEvent = monthlyCustomEvents.any { it.date == dateStr }
                                        val hasMemo = monthlyMemos.any { it.date == dateStr }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.9f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                                )
                                                .clickable { onDaySelected(dateStr) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNum,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                
                                                // Indicators row (dots)
                                                Row(
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.height(6.dp)
                                                ) {
                                                    if (hasSchoolEvent) {
                                                        Canvas(modifier = Modifier.size(4.dp).padding(horizontal = 0.5.dp)) {
                                                            drawCircle(color = Color(0xFF1E88E5)) // Blue for NEIS events
                                                        }
                                                    }
                                                    if (hasCustomEvent) {
                                                        Canvas(modifier = Modifier.size(4.dp).padding(horizontal = 0.5.dp)) {
                                                            drawCircle(color = Color(0xFFFFA000)) // Amber for Custom events
                                                        }
                                                    }
                                                    if (hasMemo) {
                                                        Canvas(modifier = Modifier.size(4.dp).padding(horizontal = 0.5.dp)) {
                                                            drawCircle(color = Color(0xFF8E24AA)) // Purple for Class Memos
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// Natural Language "Add Custom Event" Dialog with Live Feedback
// ------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var timeStr by remember { mutableStateOf("") }
    var syncToSystem by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Live detection feedback
    val parsedTime = remember(title) { TimeExtractor.extractTime(title) }
    
    // System Calendar Permissions Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val writeGranted = perms[Manifest.permission.WRITE_CALENDAR] == true
        syncToSystem = writeGranted
        if (!writeGranted) {
            Toast.makeText(context, "시스템 일정 연동을 위해선 캘린더 권한 허용이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isEmpty()) {
                        Toast.makeText(context, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalTime = if (parsedTime != null) {
                        String.format("%02d:%02d", parsedTime.first, parsedTime.second)
                    } else {
                        timeStr
                    }
                    onSave(title, finalTime, syncToSystem)
                },
                modifier = Modifier.testTag("dialog_save_button")
            ) {
                Text("일정 저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        title = {
            Text("새로운 스마트 일정 추가", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("일정 제목 (자연어 시간 가능)") },
                    placeholder = { Text("예: 수학과외 오후 3시 반") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_event_title_input"),
                    singleLine = true
                )

                // Live parsing banner
                AnimatedVisibility(
                    visible = parsedTime != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (parsedTime != null) {
                        val formattedParsed = String.format(
                            "오%s %02d시 %02d분",
                            if (parsedTime.first >= 12) "후" else "전",
                            if (parsedTime.first > 12) parsedTime.first - 12 else if (parsedTime.first == 0) 12 else parsedTime.first,
                            parsedTime.second
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "자동 감지된 시간: $formattedParsed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                if (parsedTime == null) {
                    OutlinedTextField(
                        value = timeStr,
                        onValueChange = { timeStr = it },
                        label = { Text("시간 (선택 사항)") },
                        placeholder = { Text("예: 15:30") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_event_time_input"),
                        singleLine = true
                    )
                }

                // Android OS default calendar integration switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (syncToSystem) {
                                syncToSystem = false
                            } else {
                                // Request dynamic permissions
                                val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                                val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                                if (hasWrite && hasRead) {
                                    syncToSystem = true
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)
                                    )
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("기기 시스템 캘린더 연동", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("저장 시 구글/순정 캘린더 앱에 동기화합니다.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = syncToSystem,
                        onCheckedChange = null // Click is handled by parent Row
                    )
                }
            }
        }
    )
}

// ==========================================
// 4. Configuration/Settings Panel Dialog
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    config: SchoolConfig,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(config.schulNm) }
    var selectedSchool by remember { mutableStateOf<SchoolConfig?>(config) }
    var gradeText by remember { mutableStateOf(config.grade) }
    var classText by remember { mutableStateOf(config.classNm) }

    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.schoolSearchResults.collectAsState()
    
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val school = selectedSchool
                    if (school != null && gradeText.isNotEmpty() && classText.isNotEmpty()) {
                        viewModel.saveSchoolSettings(
                            schoolName = school.schulNm,
                            officeCode = school.atptOfcdcScCode,
                            schoolCode = school.sdSchulCode,
                            kind = school.schoolKind,
                            grade = gradeText,
                            classNm = classText
                        )
                        Toast.makeText(context, "학적 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("settings_save_button")
            ) {
                Text("설정 적용")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        title = { Text("학적 정보 및 환경설정", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchSchools(it)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("settings_school_input"),
                    label = { Text("학교 재검색") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (searchResults.isNotEmpty() && searchQuery != selectedSchool?.schulNm) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(searchResults) { school ->
                                Text(
                                    text = school.schulNm,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedSchool = school
                                            searchQuery = school.schulNm
                                        }
                                        .padding(12.dp)
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = gradeText,
                        onValueChange = { gradeText = it },
                        modifier = Modifier.weight(1f).testTag("settings_grade_input"),
                        label = { Text("학년") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = classText,
                        onValueChange = { classText = it },
                        modifier = Modifier.weight(1f).testTag("settings_class_input"),
                        label = { Text("반") },
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("화면 스타일 (Theme)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeButton(
                        text = "라이트",
                        selected = config.themeMode == "LIGHT",
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.updateThemeMode("LIGHT")
                    }
                    ThemeButton(
                        text = "다크",
                        selected = config.themeMode == "DARK",
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.updateThemeMode("DARK")
                    }
                    ThemeButton(
                        text = "시스템",
                        selected = config.themeMode == "SYSTEM",
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.updateThemeMode("SYSTEM")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Security Key Disclosure Check
                Text(
                    text = "🔒 개인 데이터 암호화 활성화됨. (AES-256 PBKDF2 및 일회용 메모리 영구 파기 보장 구조)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
fun ThemeButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==========================================
// Generic UI Helper Components
// ==========================================
@Composable
fun EmptyListPlaceholder(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

fun formatPrettifiedDate(dateStr: String): String {
    if (dateStr.length != 8) return dateStr
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val format = SimpleDateFormat("yyyy년 M월 d일(E)", Locale.KOREA)
    return try {
        val date = sdf.parse(dateStr)
        if (date != null) format.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}
