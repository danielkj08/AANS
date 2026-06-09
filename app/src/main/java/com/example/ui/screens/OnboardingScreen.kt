package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SchoolConfig
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) }
    
    // School Selection states
    var searchQuery by remember { mutableStateOf("") }
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.schoolSearchResults.collectAsState()
    var selectedSchool by remember { mutableStateOf<SchoolConfig?>(null) }
    
    var gradeText by remember { mutableStateOf("1") }
    var classText by remember { mutableStateOf("1") }
    
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (step) {
            1 -> {
                OnboardingStepWelcome(
                    title = "영양 팍팍, 맞춤 급식 정보",
                    description = "풍부한 학교 급식 메뉴를 모바일 일지로 기록합니다. 무선 데이터 연결이 감지되면 한 달치 일정을 통째로 백업하는 스마트 데이터 저축(Offline Offline Caching) 기술 탑재!",
                    icon = Icons.Default.RestaurantMenu,
                    buttonText = "다음으로",
                    onNext = { step = 2 }
                )
            }
            2 -> {
                OnboardingStepWelcome(
                    title = "인공지능 추천과 자연어 분석",
                    description = "급식을 클릭하기만 하면 Gemini AI가 음식 영양 해설과 푸드 상식 💡 전수!\n추가로 일정 입력할 때 \"오후 3시 학원\"처럼 쓰고 말하면, AI가 시간을 알아서 찾아내 시간 선택 상태를 미리 완성해 줍니다! ✨",
                    icon = Icons.Default.Psychology,
                    buttonText = "내 학교 찾기",
                    onNext = { step = 3 }
                )
            }
            3 -> {
                // School Finder step
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .testTag("school_finder_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "재학 중인 학교를 찾으세요",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NEIS 교육청 통합 데이터베이스에서 실시간 검색합니다.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.searchSchools(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("school_search_input"),
                            label = { Text("학교 이름 입력 (예: 한성고)") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else if (searchResults.isEmpty()) {
                                Text(
                                    text = if (searchQuery.isEmpty()) "학교 검색어를 입력해 주세요" else "검색 결과가 없습니다",
                                    modifier = Modifier.align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(searchResults) { school ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedSchool = school }
                                                .background(
                                                    if (selectedSchool?.sdSchulCode == school.sdSchulCode)
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else Color.Transparent
                                                )
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.School,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = school.schulNm,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = school.schoolKind,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (selectedSchool != null) {
                            Text(
                                text = "선택됨: ${selectedSchool!!.schulNm}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = gradeText,
                                    onValueChange = { gradeText = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("grade_input"),
                                    label = { Text("학년") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = classText,
                                    onValueChange = { classText = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("class_input"),
                                    label = { Text("반") },
                                    singleLine = true
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = {
                                    val school = selectedSchool
                                    if (school != null && gradeText.isNotEmpty() && classText.isNotEmpty()) {
                                        viewModel.completeOnboarding(school, gradeText, classText)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("onboarding_complete_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("설정 완료하고 시작하기", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingStepWelcome(
    title: String,
    description: String,
    icon: ImageVector,
    buttonText: String,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = description,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_next_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
