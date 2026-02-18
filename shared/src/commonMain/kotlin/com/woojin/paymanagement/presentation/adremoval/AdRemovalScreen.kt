package com.woojin.paymanagement.presentation.adremoval

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.PlatformBackHandler

/**
 * 광고 제거 화면
 * 광고를 제거할 수 있는 기간별 옵션을 제공합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdRemovalScreen(
    viewModel: AdRemovalViewModel,
    onNavigateBack: () -> Unit,
    onRequestRestart: (() -> Unit)? = null
) {
    // Android 뒤로가기 버튼 처리
    PlatformBackHandler(onBack = onNavigateBack)

    val strings = LocalStrings.current
    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.removeAds) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.goBack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 제목
            Text(
                text = strings.useWithoutAds,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 설명
            Text(
                text = strings.adRemovalLongDesc,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // 광고 제거 옵션들
            AdRemovalOption(
                emoji = "📅",
                title = strings.oneDayPass,
                period = AdRemovalPeriod.ONE_DAY,
                isSelected = uiState.selectedPeriod == AdRemovalPeriod.ONE_DAY,
                onClick = { viewModel.selectAdRemovalPeriod(AdRemovalPeriod.ONE_DAY) }
            )

            AdRemovalOption(
                emoji = "📆",
                title = strings.threeDayPass,
                period = AdRemovalPeriod.THREE_DAYS,
                isSelected = uiState.selectedPeriod == AdRemovalPeriod.THREE_DAYS,
                onClick = { viewModel.selectAdRemovalPeriod(AdRemovalPeriod.THREE_DAYS) }
            )

            AdRemovalOption(
                emoji = "🗓️",
                title = strings.sevenDayPass,
                period = AdRemovalPeriod.SEVEN_DAYS,
                isSelected = uiState.selectedPeriod == AdRemovalPeriod.SEVEN_DAYS,
                onClick = { viewModel.selectAdRemovalPeriod(AdRemovalPeriod.SEVEN_DAYS) }
            )

            AdRemovalOption(
                emoji = "📅",
                title = strings.thirtyDayPass,
                period = AdRemovalPeriod.THIRTY_DAYS,
                isSelected = uiState.selectedPeriod == AdRemovalPeriod.THIRTY_DAYS,
                onClick = { viewModel.selectAdRemovalPeriod(AdRemovalPeriod.THIRTY_DAYS) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 결제 버튼
            Button(
                onClick = { viewModel.purchaseAdRemoval() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState.selectedPeriod != null && !uiState.isPurchasing,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        uiState.isPurchasing -> strings.processingPayment
                        uiState.selectedPeriod != null -> strings.payAmountButton(uiState.selectedPeriod.krw.toString())
                        else -> strings.selectPeriod
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // 성공 다이얼로그
    if (uiState.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissSuccessDialog()
            },
            icon = {
                Text(
                    text = "✅",
                    style = MaterialTheme.typography.displayMedium
                )
            },
            title = {
                Text(
                    text = strings.purchaseComplete,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = strings.adRemovalCompleteMessage,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissSuccessDialog()
                    // 앱 재시작으로 광고 제거 반영
                    if (onRequestRestart != null) {
                        onRequestRestart()
                    } else {
                        onNavigateBack()
                    }
                }) {
                    Text("확인")
                }
            }
        )
    }

    // 에러 다이얼로그
    uiState.purchaseError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            icon = {
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.displayMedium
                )
            },
            title = {
                Text(
                    text = "결제 실패",
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = error,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("확인")
                }
            }
        )
    }
}

/**
 * 광고 제거 옵션 카드 컴포넌트
 */
@Composable
private fun AdRemovalOption(
    emoji: String,
    title: String,
    period: AdRemovalPeriod,
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
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
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
                text = "₩${period.krw}",
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
