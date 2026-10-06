package com.woojin.paymanagement.presentation.monthlycomparison

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.woojin.paymanagement.theme.InvestmentColor
import com.woojin.paymanagement.theme.SavingColor
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
import com.woojin.paymanagement.domain.repository.SharedModeManager
import com.woojin.paymanagement.utils.BackHandler
import com.woojin.paymanagement.utils.Utils
import kotlin.math.round

private enum class ComparisonTab { INCOME, EXPENSE, SAVING, INVESTMENT }

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

    val hasIncome = uiState.totalCurrentIncome > 0 || uiState.totalPreviousIncome > 0
    val hasSaving = uiState.totalCurrentSaving > 0 || uiState.totalPreviousSaving > 0
    val hasInvestment = uiState.totalCurrentInvestment > 0 || uiState.totalPreviousInvestment > 0

    // 선택된 탭에 해당하는 데이터가 없으면 EXPENSE로 리셋
    LaunchedEffect(hasIncome, hasSaving, hasInvestment) {
        if (selectedTab == ComparisonTab.INCOME && !hasIncome) selectedTab = ComparisonTab.EXPENSE
        if (selectedTab == ComparisonTab.SAVING && !hasSaving) selectedTab = ComparisonTab.EXPENSE
        if (selectedTab == ComparisonTab.INVESTMENT && !hasInvestment) selectedTab = ComparisonTab.EXPENSE
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

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
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Period Navigation
            PeriodNavigationCard(
                currentPeriod = uiState.currentMonth,
                previousPeriod = uiState.previousMonth,
                onPreviousPeriod = { viewModel.moveToPreviousPeriod() },
                onNextPeriod = { viewModel.moveToNextPeriod() },
                canNavigateNext = viewModel.canNavigateNext() && !SharedModeManager.isSharedMode,
                canNavigatePrevious = !SharedModeManager.isSharedMode
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
                // ── 상단: 지출/저축/투자 탭 (데이터 있는 것만) ──
                val availableTabs = listOfNotNull(
                    ComparisonTab.INCOME.takeIf { hasIncome },
                    ComparisonTab.EXPENSE,
                    ComparisonTab.SAVING.takeIf { hasSaving },
                    ComparisonTab.INVESTMENT.takeIf { hasInvestment }
                )
                if (availableTabs.size >= 2) {
                    ComparisonTabBar(
                        tabs = availableTabs,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── 선택된 탭의 총합 비교 (한 문장 + 이전/현재 막대) ──
                when (selectedTab) {
                    ComparisonTab.INCOME -> SummaryComparisonCard(
                        tab = ComparisonTab.INCOME,
                        currentTotal = uiState.totalCurrentIncome,
                        previousTotal = uiState.totalPreviousIncome,
                        difference = uiState.totalIncomeDifference,
                        differencePercentage = uiState.totalIncomeDifferencePercentage,
                        increaseIsBad = false
                    )
                    ComparisonTab.EXPENSE -> SummaryComparisonCard(
                        tab = ComparisonTab.EXPENSE,
                        currentTotal = uiState.totalCurrentMonth,
                        previousTotal = uiState.totalPreviousMonth,
                        difference = uiState.totalDifference,
                        differencePercentage = uiState.totalDifferencePercentage,
                        increaseIsBad = true
                    )
                    ComparisonTab.SAVING -> SummaryComparisonCard(
                        tab = ComparisonTab.SAVING,
                        currentTotal = uiState.totalCurrentSaving,
                        previousTotal = uiState.totalPreviousSaving,
                        difference = uiState.totalSavingDifference,
                        differencePercentage = uiState.totalSavingDifferencePercentage,
                        increaseIsBad = false
                    )
                    ComparisonTab.INVESTMENT -> SummaryComparisonCard(
                        tab = ComparisonTab.INVESTMENT,
                        currentTotal = uiState.totalCurrentInvestment,
                        previousTotal = uiState.totalPreviousInvestment,
                        difference = uiState.totalInvestmentDifference,
                        differencePercentage = uiState.totalInvestmentDifferencePercentage,
                        increaseIsBad = false
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                ComparisonSectionBand()
                Spacer(modifier = Modifier.height(18.dp))

                // ── 하단: 선택된 탭의 카테고리별 비교 ──
                val currentComparisons = when (selectedTab) {
                    ComparisonTab.INCOME -> uiState.incomeCategoryComparisons
                    ComparisonTab.EXPENSE -> uiState.categoryComparisons
                    ComparisonTab.SAVING -> uiState.savingCategoryComparisons
                    ComparisonTab.INVESTMENT -> uiState.investmentCategoryComparisons
                }
                val increaseIsBad = selectedTab == ComparisonTab.EXPENSE

                if (currentComparisons.isNotEmpty()) {
                    Text(
                        text = strings.categoryComparison,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val midAdIndex = if (hasNativeAd && currentComparisons.size >= 6) currentComparisons.size / 2 else -1

                    currentComparisons.forEachIndexed { index, comparison ->
                        // 중간 광고 삽입 (5개 이상일 때 중간에 1번)
                        if (index == midAdIndex && nativeAdContent != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            nativeAdContent()
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        CategoryComparisonCard(
                            comparison = comparison,
                            availableCategories = uiState.availableCategories,
                            increaseIsBad = increaseIsBad
                        )
                    }

                } else {
                    Text(
                        text = strings.noComparisonData,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }

                // 하단 광고 (아이템 개수와 무관하게 항상 표시)
                if (hasNativeAd && nativeAdContent != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    nativeAdContent()
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private const val ComparisonAnimMs = 280

@Composable
private fun typeColorOf(tab: ComparisonTab): Color = when (tab) {
    ComparisonTab.INCOME -> MaterialTheme.colorScheme.primary
    ComparisonTab.EXPENSE -> MaterialTheme.colorScheme.error
    ComparisonTab.SAVING -> SavingColor.color
    ComparisonTab.INVESTMENT -> InvestmentColor.color
}

/**
 * 화면 좌우 끝까지 이어지는 회색 구분 띠 (메인 화면과 동일)
 */
@Composable
private fun ComparisonSectionBand(horizontalBleed: Dp = 16.dp) {
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val extra = horizontalBleed.roundToPx() * 2
                val width = constraints.maxWidth + extra
                val placeable = measurable.measure(
                    constraints.copy(minWidth = width, maxWidth = width)
                )
                layout(constraints.maxWidth, placeable.height) {
                    placeable.place(-extra / 2, 0)
                }
            }
            .height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun PeriodNavigationCard(
    currentPeriod: String,
    previousPeriod: String,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    canNavigateNext: Boolean,
    canNavigatePrevious: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousPeriod, enabled = canNavigatePrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = if (canNavigatePrevious) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$previousPeriod  vs",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currentPeriod,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        IconButton(onClick = onNextPeriod, enabled = canNavigateNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (canNavigateNext) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * 지출 / 저축 / 투자 세그먼트 탭 (선택 글자는 유형 색)
 */
@Composable
private fun ComparisonTabBar(
    tabs: List<ComparisonTab>,
    selectedTab: ComparisonTab,
    onTabSelected: (ComparisonTab) -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (tab) {
                        ComparisonTab.INCOME -> strings.income
                        ComparisonTab.EXPENSE -> strings.expense
                        ComparisonTab.SAVING -> strings.saving
                        ComparisonTab.INVESTMENT -> strings.investment
                    },
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) typeColorOf(tab) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 총합 비교: "지난 기간보다 / ○○원 덜 썼어요" + 증감률 칩 + 이전/현재 막대
 */
@Composable
private fun SummaryComparisonCard(
    tab: ComparisonTab,
    currentTotal: Double,
    previousTotal: Double,
    difference: Double,
    differencePercentage: Float,
    increaseIsBad: Boolean
) {
    val strings = LocalStrings.current

    val increaseColor = if (increaseIsBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val decreaseColor = if (increaseIsBad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val diffColor = when {
        difference > 0 -> increaseColor
        difference < 0 -> decreaseColor
        else -> MaterialTheme.colorScheme.onSurface
    }
    val sentenceSuffix = when (tab) {
        ComparisonTab.INCOME -> if (difference > 0) strings.earnedMore else strings.earnedLess
        ComparisonTab.EXPENSE -> if (difference > 0) strings.spentMore else strings.spentLess
        ComparisonTab.SAVING -> if (difference > 0) strings.savedMore else strings.savedLess
        ComparisonTab.INVESTMENT -> if (difference > 0) strings.investedMore else strings.investedLess
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        if (difference == 0.0) {
            Text(
                text = strings.sameAsPreviousPeriod,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = strings.comparedToPreviousPeriod,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = diffColor)) {
                        append(strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(difference))))
                    }
                    append(" ")
                    append(sentenceSuffix)
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (differencePercentage != 0f) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(diffColor.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${if (differencePercentage > 0) "↑" else "↓"} ${formatToOneDecimal(kotlin.math.abs(differencePercentage))}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = diffColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 이전 / 현재 막대 (큰 쪽을 100%로)
        val maxValue = maxOf(previousTotal, currentTotal)
        ComparisonBarRow(
            label = strings.previous,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            amount = previousTotal,
            fraction = if (maxValue > 0) (previousTotal / maxValue).toFloat() else 0f,
            barColor = MaterialTheme.colorScheme.outlineVariant,
            amountColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        ComparisonBarRow(
            label = strings.current,
            labelColor = typeColorOf(tab),
            amount = currentTotal,
            fraction = if (maxValue > 0) (currentTotal / maxValue).toFloat() else 0f,
            barColor = typeColorOf(tab),
            amountColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ComparisonBarRow(
    label: String,
    labelColor: Color,
    amount: Double,
    fraction: Float,
    barColor: Color,
    amountColor: Color
) {
    val strings = LocalStrings.current
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(ComparisonAnimMs),
        label = "comparisonBar"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            modifier = Modifier.width(40.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (animatedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction.coerceAtLeast(0.02f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(barColor)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = strings.amountWithUnit(Utils.formatAmount(amount)),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = amountColor
        )
    }
}

/**
 * 카테고리 비교 줄: 타일 + 이름 / "이전 → 현재" + 오른쪽 증감
 */
@Composable
private fun CategoryComparisonCard(
    comparison: CategoryComparison,
    availableCategories: List<com.woojin.paymanagement.data.Category>,
    increaseIsBad: Boolean = true
) {
    val strings = LocalStrings.current
    val increaseColor = if (increaseIsBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val decreaseColor = if (increaseIsBad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val diffColor = when {
        comparison.isIncrease -> increaseColor
        comparison.isDecrease -> decreaseColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val categoryEmoji = getCategoryEmoji(comparison.categoryName, availableCategories)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(diffColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (categoryEmoji.isNotBlank()) {
                Text(text = categoryEmoji, fontSize = 19.sp)
            } else {
                Text(
                    text = comparison.categoryName.take(1),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = diffColor
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comparison.categoryName,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildAnnotatedString {
                    append(strings.amountWithUnit(Utils.formatAmount(comparison.previousMonthAmount)))
                    append(" → ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                        append(strings.amountWithUnit(Utils.formatAmount(comparison.currentMonthAmount)))
                    }
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (comparison.isUnchanged) {
                    strings.amountWithUnit("0")
                } else {
                    "${if (comparison.isIncrease) "↑" else "↓"} ${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(comparison.difference)))}"
                },
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = diffColor
            )
            if (comparison.differencePercentage != 0f) {
                Text(
                    text = "${if (comparison.differencePercentage > 0) "+" else ""}${formatToOneDecimal(comparison.differencePercentage)}%",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = diffColor
                )
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
