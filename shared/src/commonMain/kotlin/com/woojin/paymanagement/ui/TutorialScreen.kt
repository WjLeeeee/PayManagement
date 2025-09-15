package com.woojin.paymanagement.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    onTutorialComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1) / 3f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Color.Gray,
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> TutorialPage1()
                1 -> TutorialPage2()
                2 -> TutorialPage3()
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Page indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color.Gray else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Previous button
            if (pagerState.currentPage > 0) {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("이전", color = Color.Black)
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp)) // Placeholder
            }
            
            // Next/Complete button
            if (pagerState.currentPage < 2) {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("다음", color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black)
                }
            } else {
                Button(
                    onClick = onTutorialComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("시작하기", color = Color.Black)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TutorialPage1() {
    TutorialPageLayout(
        icon = Icons.Default.Home,
        title = "급여 기간별 가계부에\n오신 것을 환영합니다!",
        description = "이 앱은 일반적인 월별 관리가 아닌\n급여일 기준으로 가계를 관리할 수 있습니다.",
        details = listOf(
            "✓ 급여일부터 다음 급여일 전날까지를 한 주기로 관리",
            "✓ 실제 생활 패턴에 맞는 가계부 작성",
            "✓ 더 정확한 월별 수입/지출 분석"
        ),
        iconColor = Color.Blue
    )
}

@Composable
private fun TutorialPage2() {
    TutorialPageLayout(
        icon = Icons.Default.Star,
        title = "급여 기간 요약으로\n한눈에 확인하세요",
        description = "메인 화면의 급여 기간 요약 카드를\n클릭하면 상세한 통계를 볼 수 있습니다.",
        details = listOf(
            "📊 수입/지출 비율을 그래프로 확인",
            "📈 카테고리별 지출 패턴 분석",
            "💰 급여 기간별 수지 현황 파악"
        ),
        iconColor = Color.Green
    )
}

@Composable
private fun TutorialPage3() {
    TutorialPageLayout(
        icon = Icons.Default.Add,
        title = "일별 거래 내역으로\n세부 관리하세요",
        description = "날짜별 거래 내역 카드를 클릭하면\n상세 화면에서 내역을 추가할 수 있습니다.",
        details = listOf(
            "📝 날짜별 상세 거래 내역 확인",
            "➕ 수입/지출 내역 바로 추가",
            "✏️ 기존 내역 수정 및 삭제 가능"
        ),
        iconColor = Color(0xFFFF9800)
    )
}

@Composable
private fun TutorialPageLayout(
    icon: ImageVector,
    title: String,
    description: String,
    details: List<String>,
    iconColor: Color
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Card(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = iconColor.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = iconColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.LightGray)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                details.forEach { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}