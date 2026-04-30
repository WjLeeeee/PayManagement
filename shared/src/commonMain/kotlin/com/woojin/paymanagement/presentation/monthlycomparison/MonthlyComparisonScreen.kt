package com.woojin.paymanagement.presentation.monthlycomparison

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.BackHandler
import com.woojin.paymanagement.utils.Utils
import kotlin.math.round

private enum class ComparisonTab { EXPENSE, SAVING, INVESTMENT }

@Composable
fun MonthlyComparisonScreen(
    viewModel: MonthlyComparisonViewModel,
    onBack: () -> Unit,
    showPreviousPeriodComparison: Boolean = false,
    nativeAdContent: @Composable (() -> Unit)? = null,
    hasNativeAd: Boolean = false
) {
    val strings = LocalStrings.current
    BackHandler(onBack = onBack)

    val uiState = viewModel.uiState

    var selectedTab by remember { mutableStateOf(ComparisonTab.EXPENSE) }

    // 스낵바에서 진입한 경우 이전 급여 기간 비교 모드로 시작
    LaunchedEffect(showPreviousPeriodComparison) {
        if (showPreviousPeriodComparison) {
            viewModel.startWithPreviousPeriod()
        }
    }

    val hasSaving = uiState.totalCurrentSaving > 0 || uiState.totalPreviousSaving > 0
    val hasInvestment = uiState.totalCurrentInvestment > 0 || uiState.totalPreviousInvestment > 0

    // 선택된 탭에 해당하는 데이터가 없으면 EXPENSE로 리셋
    LaunchedEffect(hasSaving, hasInvestment) {
        if (selectedTab == ComparisonTab.SAVING && !hasSaving) selectedTab = ComparisonTab.EXPENSE
        if (selectedTab == ComparisonTab.INVESTMENT && !hasInvestment) selectedTab = ComparisonTab.EXPENSE
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.goBack,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = strings.payPeriodComparison,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Period Navigation
            PeriodNavigationCard(
                currentPeriod = uiState.currentMonth,
                previousPeriod = uiState.previousMonth,
                onPreviousPeriod = { viewModel.moveToPreviousPeriod() },
                onNextPeriod = { viewModel.moveToNextPeriod() },
                canNavigateNext = viewModel.canNavigateNext()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // ── 상단: 총합 비교 카드들 (선택 가능) ──
                SummaryComparisonCard(
                    title = strings.totalExpenseComparison,
                    currentTotal = uiState.totalCurrentMonth,
                    previousTotal = uiState.totalPreviousMonth,
                    difference = uiState.totalDifference,
                    differencePercentage = uiState.totalDifferencePercentage,
                    increaseIsBad = true,
                    isSelected = selectedTab == ComparisonTab.EXPENSE,
                    onClick = { selectedTab = ComparisonTab.EXPENSE }
                )

                if (hasSaving) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SummaryComparisonCard(
                        title = strings.totalSavingComparison,
                        currentTotal = uiState.totalCurrentSaving,
                        previousTotal = uiState.totalPreviousSaving,
                        difference = uiState.totalSavingDifference,
                        differencePercentage = uiState.totalSavingDifferencePercentage,
                        increaseIsBad = false,
                        isSelected = selectedTab == ComparisonTab.SAVING,
                        onClick = { selectedTab = ComparisonTab.SAVING }
                    )
                }

                if (hasInvestment) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SummaryComparisonCard(
                        title = strings.totalInvestmentComparison,
                        currentTotal = uiState.totalCurrentInvestment,
                        previousTotal = uiState.totalPreviousInvestment,
                        difference = uiState.totalInvestmentDifference,
                        differencePercentage = uiState.totalInvestmentDifferencePercentage,
                        increaseIsBad = false,
                        isSelected = selectedTab == ComparisonTab.INVESTMENT,
                        onClick = { selectedTab = ComparisonTab.INVESTMENT }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── 하단: 선택된 탭의 카테고리별 비교 ──
                val currentComparisons = when (selectedTab) {
                    ComparisonTab.EXPENSE -> uiState.categoryComparisons
                    ComparisonTab.SAVING -> uiState.savingCategoryComparisons
                    ComparisonTab.INVESTMENT -> uiState.investmentCategoryComparisons
                }
                val increaseIsBad = selectedTab == ComparisonTab.EXPENSE

                if (currentComparisons.isNotEmpty()) {
                    Text(
                        text = strings.categoryComparison,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val midAdIndex = if (hasNativeAd && currentComparisons.size >= 6) currentComparisons.size / 2 else -1

                    currentComparisons.forEachIndexed { index, comparison ->
                        // 중간 광고 삽입 (5개 이상일 때 중간에 1번)
                        if (index == midAdIndex && nativeAdContent != null) {
                            nativeAdContent()
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        CategoryComparisonCard(
                            comparison = comparison,
                            availableCategories = uiState.availableCategories,
                            increaseIsBad = increaseIsBad
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                } else {
                    Text(
                        text = strings.noComparisonData,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }

                // 하단 광고 (아이템 개수와 무관하게 항상 표시)
                if (hasNativeAd && nativeAdContent != null) {
                    nativeAdContent()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PeriodNavigationCard(
    currentPeriod: String,
    previousPeriod: String,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    canNavigateNext: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPreviousPeriod) {
                    Text("◀", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = previousPeriod,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "vs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = currentPeriod,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                TextButton(
                    onClick = onNextPeriod,
                    enabled = canNavigateNext
                ) {
                    Text(
                        "▶",
                        fontSize = 24.sp,
                        color = if (canNavigateNext)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryComparisonCard(
    title: String,
    currentTotal: Double,
    previousTotal: Double,
    difference: Double,
    differencePercentage: Float,
    increaseIsBad: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "borderColor"
    )
    val increaseColor = if (increaseIsBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val decreaseColor = if (increaseIsBad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // 이전 기간
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(
                        text = strings.previous,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(previousTotal)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 현재 기간
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.current,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(currentTotal)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 차이
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    val diffColor = when {
                        difference > 0 -> increaseColor
                        difference < 0 -> decreaseColor
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = when {
                            difference > 0 -> strings.increasedArrow
                            difference < 0 -> strings.savingsArrow
                            else -> strings.same
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = diffColor
                    )
                    Text(
                        text = if (difference == 0.0) {
                            strings.amountWithUnit("0")
                        } else {
                            "${if (difference > 0) "+" else ""}${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(difference)))}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = diffColor
                    )
                    if (differencePercentage != 0f) {
                        Text(
                            text = "(${if (differencePercentage > 0) "+" else ""}${formatToOneDecimal(differencePercentage)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = diffColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryComparisonCard(
    comparison: CategoryComparison,
    availableCategories: List<com.woojin.paymanagement.data.Category>,
    increaseIsBad: Boolean = true
) {
    val strings = LocalStrings.current
    val increaseColor = if (increaseIsBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val decreaseColor = if (increaseIsBad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = getCategoryEmoji(comparison.categoryName, availableCategories),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = comparison.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(
                        text = strings.previous,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(comparison.previousMonthAmount)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.current,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(comparison.currentMonthAmount)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    val diffColor = when {
                        comparison.isIncrease -> increaseColor
                        comparison.isDecrease -> decreaseColor
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    if (!comparison.isUnchanged) {
                        Text(
                            text = if (comparison.isIncrease) "↑" else "↓",
                            style = MaterialTheme.typography.bodySmall,
                            color = diffColor
                        )
                    }
                    Text(
                        text = if (comparison.isUnchanged) {
                            strings.amountWithUnit("0")
                        } else {
                            "${if (comparison.difference > 0) "+" else ""}${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(comparison.difference)))}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = diffColor
                    )
                    if (comparison.differencePercentage != 0f) {
                        Text(
                            text = "(${if (comparison.differencePercentage > 0) "+" else ""}${formatToOneDecimal(comparison.differencePercentage)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = diffColor
                        )
                    }
                }
            }
        }
    }
}

private fun formatToOneDecimal(value: Float): String {
    val rounded = round(value * 10) / 10
    return if (rounded == rounded.toInt().toFloat()) {
        "${rounded.toInt()}.0"
    } else {
        rounded.toString()
    }
}
