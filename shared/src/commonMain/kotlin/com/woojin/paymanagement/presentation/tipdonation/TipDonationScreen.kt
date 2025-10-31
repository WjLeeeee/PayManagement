package com.woojin.paymanagement.presentation.tipdonation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.utils.PlatformBackHandler

/**
 * 팁 주기 화면
 * 개발자를 응원할 수 있는 기부 옵션을 제공합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipDonationScreen(
    onNavigateBack: () -> Unit
) {
    // Android 뒤로가기 버튼 처리
    PlatformBackHandler(onBack = onNavigateBack)

    var selectedTipAmount by remember { mutableStateOf<TipAmount?>(null) }
    var showThankYouDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("팁주기") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 제목
            Text(
                text = "개발자 응원하기",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 설명
            Text(
                text = "앱이 도움이 되셨나요?\n개발자에게 커피 한 잔 사주세요!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // 팁 옵션들
            TipOption(
                emoji = "☕",
                title = "커피 사주기",
                amount = TipAmount.COFFEE,
                isSelected = selectedTipAmount == TipAmount.COFFEE,
                onClick = { selectedTipAmount = TipAmount.COFFEE }
            )

            TipOption(
                emoji = "🍱",
                title = "점심 사주기",
                amount = TipAmount.LUNCH,
                isSelected = selectedTipAmount == TipAmount.LUNCH,
                onClick = { selectedTipAmount = TipAmount.LUNCH }
            )

            TipOption(
                emoji = "🍽️",
                title = "저녁 사주기",
                amount = TipAmount.DINNER,
                isSelected = selectedTipAmount == TipAmount.DINNER,
                onClick = { selectedTipAmount = TipAmount.DINNER }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 결제 버튼
            Button(
                onClick = {
                    selectedTipAmount?.let {
                        // TODO: 실제 결제 연동 (Google Play Billing)
                        showThankYouDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedTipAmount != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = selectedTipAmount?.let { "₩${it.krw} 결제하기" } ?: "금액을 선택해주세요",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 안내 문구
            Text(
                text = "※ 현재 결제 기능은 준비 중입니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }

    // 감사 다이얼로그
    if (showThankYouDialog) {
        AlertDialog(
            onDismissRequest = {
                showThankYouDialog = false
                onNavigateBack()
            },
            icon = {
                Text(
                    text = "🎉",
                    style = MaterialTheme.typography.displayMedium
                )
            },
            title = {
                Text(
                    text = "감사합니다!",
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "개발자를 응원해주셔서 감사합니다.\n더 좋은 앱으로 보답하겠습니다!",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showThankYouDialog = false
                    onNavigateBack()
                }) {
                    Text("확인")
                }
            }
        )
    }
}

/**
 * 팁 옵션 카드 컴포넌트
 */
@Composable
private fun TipOption(
    emoji: String,
    title: String,
    amount: TipAmount,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "₩${amount.krw}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 팁 금액 옵션
 */
enum class TipAmount(val krw: String) {
    COFFEE("1,000"),
    LUNCH("5,000"),
    DINNER("10,000")
}
