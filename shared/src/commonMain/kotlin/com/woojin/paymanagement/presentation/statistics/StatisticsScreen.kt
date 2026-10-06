package com.woojin.paymanagement.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import com.woojin.paymanagement.presentation.addtransaction.SelectablePillChip
import com.woojin.paymanagement.theme.BrandColor
import com.woojin.paymanagement.theme.InvestmentColor
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.BalanceCardSummary
import com.woojin.paymanagement.data.CardBreakdown
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.GiftCardSummary
import com.woojin.paymanagement.data.PaymentMethodSummary
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji
import com.woojin.paymanagement.presentation.components.PieChart
import com.woojin.paymanagement.domain.repository.SharedModeManager
import com.woojin.paymanagement.utils.BackHandler
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.Utils
import com.woojin.paymanagement.strings.LocalStrings
import kotlinx.coroutines.flow.collectLatest

@Composable
fun StatisticsScreen(
    transactions: List<Transaction>,
    availableBalanceCards: List<BalanceCard> = emptyList(),
    availableGiftCards: List<GiftCard> = emptyList(),
    initialPayPeriod: PayPeriod? = null,
    onBack: () -> Unit,
    viewModel: StatisticsViewModel
) {
    val strings = LocalStrings.current

    // 시스템 뒤로가기 버튼 처리
    BackHandler(onBack = onBack)

    // ViewModel의 uiState를 직접 사용
    val uiState = viewModel.uiState

    // 통계 데이터를 위한 별도 상태
    var statisticsData by remember { mutableStateOf(StatisticsUiState()) }
    var selectedTab by remember { mutableStateOf(StatTab.INCOME) }

    LaunchedEffect(initialPayPeriod, availableBalanceCards, availableGiftCards) {
        viewModel.initializeStatistics(initialPayPeriod, availableBalanceCards, availableGiftCards)

        viewModel.getStatisticsFlow(availableBalanceCards, availableGiftCards)
            .collectLatest { newState ->
                statisticsData = newState
            }
    }

    LaunchedEffect(uiState.currentPayPeriod) {
        if (uiState.currentPayPeriod != null) {
            viewModel.getStatisticsFlow(availableBalanceCards, availableGiftCards)
                .collectLatest { newState ->
                    statisticsData = newState
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header with back button
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
                text = strings.statistics,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        // Period Navigation
        uiState.currentPayPeriod?.let { currentPayPeriod ->
            PayPeriodNavigationCard(
                currentPayPeriod = currentPayPeriod,
                onPreviousPeriod = { viewModel.moveToPreviousPeriod() },
                onNextPeriod = { viewModel.moveToNextPeriod() },
                navigationEnabled = !SharedModeManager.isSharedMode
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Summary Card
        statisticsData.chartData?.let { chartData ->
            SummaryCard(
                totalIncome = chartData.totalIncome,
                totalExpense = chartData.totalExpense
            )
        }

        // Investment Summary Card
        // 전체 transactions에서 투자 관련 카테고리 데이터 계산
        val investmentTransactions = statisticsData.transactions.filter { it.category == "투자" }
        val lossCutTransactions = statisticsData.transactions.filter { it.category == "손절" }
        val profitTransactions = statisticsData.transactions.filter { it.category == "익절" }
        val dividendTransactions = statisticsData.transactions.filter { it.category == "배당금" }

        val investmentAmount = investmentTransactions.sumOf { it.displayAmount }
        val lossCutAmount = lossCutTransactions.sumOf { it.displayAmount }
        val profitAmount = profitTransactions.sumOf { it.displayAmount }
        val dividendAmount = dividendTransactions.sumOf { it.displayAmount }
        val hasInvestmentData = investmentAmount > 0 || lossCutAmount > 0 || profitAmount > 0 || dividendAmount > 0

        if (hasInvestmentData) {
            InvestmentSummaryCard(
                investmentAmount = investmentAmount,
                lossCutAmount = lossCutAmount,
                profitAmount = profitAmount,
                dividendAmount = dividendAmount
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        StatSectionBand()
        Spacer(modifier = Modifier.height(20.dp))

        // 카테고리별 탭 분기
        val hasIncome = statisticsData.chartData?.incomeItems?.isNotEmpty() == true
        val hasExpense = statisticsData.chartData?.expenseItems?.isNotEmpty() == true
        val hasSaving = statisticsData.chartData?.savingItems?.isNotEmpty() == true

        val availableTabs = listOfNotNull(
            StatTab.INCOME.takeIf { hasIncome },
            StatTab.EXPENSE.takeIf { hasExpense },
            StatTab.SAVING.takeIf { hasSaving },
            StatTab.INVESTMENT.takeIf { hasInvestmentData }
        )
        val showTabs = availableTabs.size >= 2

        LaunchedEffect(hasIncome, hasExpense, hasSaving, hasInvestmentData) {
            if (selectedTab !in availableTabs && availableTabs.isNotEmpty()) {
                selectedTab = availableTabs.first()
            }
        }

        if (showTabs) {
            StatTabBar(
                tabs = availableTabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            when (selectedTab) {
                StatTab.INCOME -> statisticsData.chartData?.let { chartData ->
                    if (chartData.incomeItems.isNotEmpty()) {
                        ChartSection(
                            title = strings.incomeAnalysis,
                            items = chartData.incomeItems,
                            total = chartData.totalIncome,
                            availableCategories = uiState.availableCategories,
                            transactions = statisticsData.transactions,
                            transactionType = TransactionType.INCOME
                        )
                    }
                }
                StatTab.EXPENSE -> statisticsData.chartData?.let { chartData ->
                    if (chartData.expenseItems.isNotEmpty()) {
                        ChartSection(
                            title = strings.expenseAnalysis,
                            items = chartData.expenseItems,
                            total = chartData.totalExpense,
                            availableCategories = uiState.availableCategories,
                            transactions = statisticsData.transactions,
                            transactionType = TransactionType.EXPENSE
                        )
                    }
                }
                StatTab.SAVING -> statisticsData.chartData?.let { chartData ->
                    if (chartData.savingItems.isNotEmpty()) {
                        ChartSection(
                            title = strings.savingActivitySummary,
                            items = chartData.savingItems,
                            total = chartData.totalSaving,
                            availableCategories = uiState.availableCategories,
                            transactions = statisticsData.transactions,
                            transactionType = TransactionType.SAVING,
                            groupSmallItems = false,
                            filterByType = true
                        )
                    }
                }
                StatTab.INVESTMENT -> statisticsData.chartData?.let { chartData ->
                    if (chartData.investmentItems.isNotEmpty()) {
                        ChartSection(
                            title = strings.investmentActivityAnalysis,
                            items = chartData.investmentItems,
                            total = chartData.totalInvestment,
                            availableCategories = uiState.availableCategories,
                            transactions = statisticsData.transactions,
                            transactionType = TransactionType.INCOME,
                            groupSmallItems = false,
                            filterByType = false
                        )
                    }
                }
            }
        } else {
            // 1개 이하 카테고리: 기존 방식대로 모두 표시
            statisticsData.chartData?.let { chartData ->
                if (chartData.incomeItems.isNotEmpty()) {
                    ChartSection(
                        title = strings.incomeAnalysis,
                        items = chartData.incomeItems,
                        total = chartData.totalIncome,
                        availableCategories = uiState.availableCategories,
                        transactions = statisticsData.transactions,
                        transactionType = TransactionType.INCOME
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            statisticsData.chartData?.let { chartData ->
                if (chartData.expenseItems.isNotEmpty()) {
                    ChartSection(
                        title = strings.expenseAnalysis,
                        items = chartData.expenseItems,
                        total = chartData.totalExpense,
                        availableCategories = uiState.availableCategories,
                        transactions = statisticsData.transactions,
                        transactionType = TransactionType.EXPENSE
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            if (hasInvestmentData) {
                statisticsData.chartData?.let { chartData ->
                    if (chartData.investmentItems.isNotEmpty()) {
                        ChartSection(
                            title = strings.investmentActivityAnalysis,
                            items = chartData.investmentItems,
                            total = chartData.totalInvestment,
                            availableCategories = uiState.availableCategories,
                            transactions = statisticsData.transactions,
                            transactionType = TransactionType.INCOME,
                            groupSmallItems = false,
                            filterByType = false
                        )
                    }
                }
            }

            statisticsData.chartData?.let { chartData ->
                if (chartData.savingItems.isNotEmpty()) {
                    ChartSection(
                        title = strings.savingActivitySummary,
                        items = chartData.savingItems,
                        total = chartData.totalSaving,
                        availableCategories = uiState.availableCategories,
                        transactions = statisticsData.transactions,
                        transactionType = TransactionType.SAVING,
                        groupSmallItems = false,
                        filterByType = true
                    )
                }
            }
        }

        // Payment Method Summary
        statisticsData.paymentSummary?.let { paymentSummary ->
            if (paymentSummary.cashIncome > 0 || paymentSummary.cashExpense > 0 || paymentSummary.cardExpense > 0 ||
                paymentSummary.balanceCards.isNotEmpty() || paymentSummary.giftCards.isNotEmpty()) {

                Spacer(modifier = Modifier.height(22.dp))
                StatSectionBand()
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = strings.paymentMethodAnalysis,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                PaymentMethodSection(paymentSummary = paymentSummary)
            }
        }

        if (statisticsData.chartData?.let { it.incomeItems.isEmpty() && it.expenseItems.isEmpty() } == true) {
            Text(
                text = strings.noTransactionsForPeriod,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private const val StatAnimMs = 280

/**
 * 화면 좌우 끝까지 이어지는 회색 구분 띠 (메인 화면과 동일)
 */
@Composable
private fun StatSectionBand(horizontalBleed: Dp = 16.dp) {
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
private fun PayPeriodNavigationCard(
    currentPayPeriod: PayPeriod,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    navigationEnabled: Boolean = true
) {
    val arrowTint = if (navigationEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousPeriod, enabled = navigationEnabled) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = arrowTint
            )
        }

        Text(
            text = currentPayPeriod.displayText,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false)
        )

        IconButton(onClick = onNextPeriod, enabled = navigationEnabled) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = arrowTint
            )
        }
    }
}

/**
 * 급여 기간 요약: 수입 | 지출 | 잔액 한 줄
 */
@Composable
private fun SummaryCard(
    totalIncome: Double,
    totalExpense: Double
) {
    val strings = LocalStrings.current
    val balance = totalIncome - totalExpense
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SummaryValue(
            label = strings.income,
            value = "+${strings.amountWithUnit(Utils.formatAmount(totalIncome))}",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        SummaryDivider()
        SummaryValue(
            label = strings.expense,
            value = "-${strings.amountWithUnit(Utils.formatAmount(totalExpense))}",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        SummaryDivider()
        SummaryValue(
            label = strings.balance,
            value = when {
                balance > 0 -> "+${strings.amountWithUnit(Utils.formatAmount(balance))}"
                balance < 0 -> "-${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(balance)))}"
                else -> strings.amountWithUnit(Utils.formatAmount(balance))
            },
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
    }
}

@Composable
private fun SummaryValue(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

/**
 * 투자 활동: 값이 있는 항목은 색 칩, 0인 항목은 회색 칩 하나로 묶음
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InvestmentSummaryCard(
    investmentAmount: Double,
    lossCutAmount: Double,
    profitAmount: Double,
    dividendAmount: Double
) {
    val strings = LocalStrings.current
    val zeroLabels = mutableListOf<String>()

    Spacer(modifier = Modifier.height(14.dp))
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = strings.investment,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 2.dp)
        )

        if (investmentAmount > 0) {
            StatChip(
                text = "${strings.investment} ${strings.amountWithUnit(Utils.formatAmount(investmentAmount))}",
                color = InvestmentColor.color
            )
        } else zeroLabels += strings.investment

        if (lossCutAmount > 0) {
            StatChip(
                text = "${strings.stopLoss} -${strings.amountWithUnit(Utils.formatAmount(lossCutAmount))}",
                color = MaterialTheme.colorScheme.error
            )
        } else zeroLabels += strings.stopLoss

        if (profitAmount > 0) {
            StatChip(
                text = "${strings.profitTaking} +${strings.amountWithUnit(Utils.formatAmount(profitAmount))}",
                color = MaterialTheme.colorScheme.primary
            )
        } else zeroLabels += strings.profitTaking

        if (dividendAmount > 0) {
            StatChip(
                text = "${strings.dividend} +${strings.amountWithUnit(Utils.formatAmount(dividendAmount))}",
                color = MaterialTheme.colorScheme.primary
            )
        } else zeroLabels += strings.dividend

        if (zeroLabels.isNotEmpty()) {
            StatChip(
                text = zeroLabels.joinToString(" · ") { "$it 0" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                background = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun StatChip(
    text: String,
    color: Color,
    background: Color = color.copy(alpha = 0.1f)
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ChartSection(
    title: String,
    items: List<com.woojin.paymanagement.data.ChartItem>,
    total: Double,
    availableCategories: List<com.woojin.paymanagement.data.Category> = emptyList(),
    transactions: List<Transaction> = emptyList(),
    transactionType: TransactionType,
    groupSmallItems: Boolean = true, // 기본값은 true (기타로 묶음)
    filterByType: Boolean = true // 기본값은 true (타입으로 필터링)
) {
    val strings = LocalStrings.current
    // 선택된 카테고리 상태 - 초기값은 가장 비율이 높은 항목
    var selectedCategory by remember(items) {
        mutableStateOf(items.maxByOrNull { it.percentage }?.category)
    }

    // "기타" 색상을 먼저 가져오기
    val etcColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 3% 미만 항목들을 "기타"로 묶기 (groupSmallItems가 true일 때만)
    val (processedItems, mainItems, smallItems) = remember(items, total, etcColor, groupSmallItems) {
        if (!groupSmallItems) {
            // 기타로 묶지 않고 모든 항목 표시
            Triple(items, items, emptyList())
        } else {
            val threshold = 3.0f
            val mainItems = items.filter { it.percentage >= threshold }
            val smallItems = items.filter { it.percentage < threshold }

            if (smallItems.isEmpty()) {
                Triple(items, items, emptyList())
            } else {
                val etcAmount = smallItems.sumOf { it.amount.toDouble() }
                val etcPercentage = smallItems.sumOf { it.percentage.toDouble() }.toFloat()

                val etcItem = com.woojin.paymanagement.data.ChartItem(
                    category = strings.other,
                    amount = etcAmount,
                    percentage = etcPercentage,
                    color = etcColor
                )

                Triple(mainItems + etcItem, mainItems, smallItems)
            }
        }
    }

    Column {
        // 제목 + 합계
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = strings.amountWithUnit(Utils.formatAmount(total)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PieChart(
            items = processedItems,
            chartSize = 120.dp,
            showLegend = false,
            labelTextColor = MaterialTheme.colorScheme.onSurface,
            valueLineColor = MaterialTheme.colorScheme.onSurface,
            selectedCategory = selectedCategory,
            onItemSelected = { category ->
                selectedCategory = category
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Legend: 주요 항목 + 기타(소항목들)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 주요 항목들 표시
            mainItems.forEach { item ->
                val categoryTransactions = transactions.filter {
                    if (filterByType) {
                        it.category == item.category && it.type == transactionType
                    } else {
                        it.category == item.category
                    }
                }.sortedBy { it.date }

                ChartLegendItem(
                    item = item,
                    isSubItem = false,
                    isSelected = selectedCategory == item.category,
                    availableCategories = availableCategories,
                    onClick = { selectedCategory = if (selectedCategory == item.category) null else item.category },
                    transactions = categoryTransactions,
                    transactionType = transactionType
                )
            }

            // 기타 항목이 있으면 표시
            if (smallItems.isNotEmpty()) {
                // "기타" 헤더
                val etcTotal = smallItems.sumOf { it.amount.toDouble() }
                val etcPercentage = smallItems.sumOf { it.percentage.toDouble() }.toFloat()

                ChartLegendItem(
                    item = com.woojin.paymanagement.data.ChartItem(
                        category = strings.other,
                        amount = etcTotal,
                        percentage = etcPercentage,
                        color = etcColor
                    ),
                    isSubItem = false,
                    isSelected = selectedCategory == strings.other,
                    availableCategories = availableCategories,
                    onClick = { selectedCategory = if (selectedCategory == strings.other) null else strings.other },
                    transactions = emptyList(),
                    transactionType = transactionType
                )

                // 기타 내부 항목들 (들여쓰기)
                smallItems.forEach { item ->
                    val categoryTransactions = transactions.filter {
                        if (filterByType) {
                            it.category == item.category && it.type == transactionType
                        } else {
                            it.category == item.category
                        }
                    }.sortedBy { it.date }

                    ChartLegendItem(
                        item = item,
                        isSubItem = true,
                        isSelected = selectedCategory == item.category,
                        availableCategories = availableCategories,
                        onClick = { selectedCategory = if (selectedCategory == item.category) null else item.category },
                        transactions = categoryTransactions,
                        transactionType = transactionType
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    item: com.woojin.paymanagement.data.ChartItem,
    isSubItem: Boolean = false,
    isSelected: Boolean = false,
    availableCategories: List<com.woojin.paymanagement.data.Category> = emptyList(),
    onClick: () -> Unit = {},
    transactions: List<Transaction> = emptyList(),
    transactionType: TransactionType
) {
    val strings = LocalStrings.current
    val rowBackground by animateColorAsState(
        targetValue = if (isSelected) item.color.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(StatAnimMs)
    )
    val categoryEmoji = getCategoryEmoji(item.category, availableCategories)
    val percentText = "${(item.percentage * 10).toInt() / 10.0}%"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isSubItem) 50.dp else 0.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(rowBackground)
            .clickable { onClick() }
            .padding(
                horizontal = 10.dp,
                vertical = if (isSubItem) 6.dp else 10.dp
            )
    ) {
        if (isSubItem) {
            // 소항목: 점 + 이름 · 금액 + 비율 (한 줄)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(item.color)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = buildString {
                        if (categoryEmoji.isNotBlank()) append("$categoryEmoji ")
                        append(item.category)
                    },
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " · ${strings.amountWithUnit(Utils.formatAmount(item.amount))}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = percentText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 아이콘 타일 (항목 색으로 은은하게)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(item.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (categoryEmoji.isNotBlank()) {
                        Text(text = categoryEmoji, fontSize = 18.sp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(item.color)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.category,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = percentText,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(item.amount)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    // 비율 막대 (기존 percentage 값을 그대로 표시)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((item.percentage / 100f).coerceIn(0.015f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(item.color)
                        )
                    }
                }
            }
        }

        // 거래 내역 확장 표시
        AnimatedVisibility(
            visible = isSelected && transactions.isNotEmpty(),
            enter = expandVertically(tween(StatAnimMs)) + fadeIn(tween(StatAnimMs)),
            exit = shrinkVertically(tween(StatAnimMs)) + fadeOut(tween(StatAnimMs))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (isSubItem) 18.dp else 50.dp, top = 10.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                transactions.forEach { transaction ->
                    val dateText = "${transaction.date.monthNumber}/${transaction.date.dayOfMonth.toString().padStart(2, '0')}"
                    val amountText = strings.amountWithUnit(Utils.formatAmount(transaction.displayAmount))

                    // 실제 거래 타입에 따라 표시 (transactionType 파라미터가 아닌 transaction.type 사용)
                    val description = when (transaction.type) {
                        // 지출: 사용처 + 메모 (있으면)
                        TransactionType.EXPENSE -> {
                            val merchant = transaction.merchant ?: ""
                            if (transaction.memo.isNotBlank()) "$merchant (${transaction.memo})" else merchant
                        }
                        // 수입/저축/투자: 메모 (있으면)
                        else -> transaction.memo
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(44.dp)
                        )
                        Text(
                            text = description,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = amountText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodSection(
    paymentSummary: PaymentMethodSummary
) {
    Column {
        // Cash Summary
        if (paymentSummary.cashIncome > 0 || paymentSummary.cashExpense > 0) {
            CashSummaryCard(
                income = paymentSummary.cashIncome,
                expense = paymentSummary.cashExpense
            )

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Card Summary
        if (paymentSummary.cardExpense > 0) {
            CardSummaryCard(
                expense = paymentSummary.cardExpense,
                actualExpense = paymentSummary.cardActualExpense,
                settlementIncome = paymentSummary.settlementIncome,
                cardBreakdowns = paymentSummary.cardBreakdowns
            )

            Spacer(modifier = Modifier.height(18.dp))
        }

        // 기타 (잔액권 + 상품권) Summary
        val otherIncome = paymentSummary.balanceCards.sumOf { it.income } +
                          paymentSummary.giftCards.sumOf { it.income }
        val otherExpense = paymentSummary.balanceCards.sumOf { it.expense } +
                           paymentSummary.giftCards.sumOf { it.expense }

        if (otherIncome > 0 || otherExpense > 0) {
            OtherPaymentSummaryCard(
                income = otherIncome,
                expense = otherExpense
            )
        }
    }
}

/**
 * 결제 수단 헤더: 이모지 타일 + 이름 (+ 오른쪽 금액)
 */
@Composable
private fun PaymentHeader(
    emoji: String,
    name: String,
    tint: Color,
    trailing: String? = null,
    trailingColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = trailingColor
            )
        }
    }
}

/**
 * 회색 상자 안의 "라벨 — 값" 줄 목록 (줄 사이 얇은 구분선)
 */
@Composable
private fun PaymentRowsBox(
    rows: List<Triple<String, String, Color>>,
    boldLastRow: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 2.dp)
    ) {
        rows.forEachIndexed { index, (label, value, color) ->
            val isBold = boldLastRow && index == rows.lastIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    fontSize = 13.5.sp,
                    fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
                    color = color
                )
            }
            if (index != rows.lastIndex) {
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun CashSummaryCard(
    income: Double,
    expense: Double
) {
    val strings = LocalStrings.current
    val balance = income - expense
    Column(modifier = Modifier.fillMaxWidth()) {
        PaymentHeader(emoji = "💰", name = strings.cash, tint = BrandColor.mint)
        PaymentRowsBox(
            rows = listOf(
                Triple(
                    strings.income,
                    "+${strings.amountWithUnit(Utils.formatAmount(income))}",
                    MaterialTheme.colorScheme.primary
                ),
                Triple(
                    strings.expense,
                    "-${strings.amountWithUnit(Utils.formatAmount(expense))}",
                    MaterialTheme.colorScheme.error
                ),
                Triple(
                    strings.differenceAmount,
                    when {
                        balance > 0 -> "+${strings.amountWithUnit(Utils.formatAmount(balance))}"
                        balance < 0 -> "-${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(balance)))}"
                        else -> strings.amountWithUnit(Utils.formatAmount(balance))
                    },
                    when {
                        balance > 0 -> MaterialTheme.colorScheme.primary
                        balance < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            ),
            boldLastRow = true
        )
    }
}

@Composable
private fun CardSummaryCard(
    expense: Double,
    actualExpense: Double,
    settlementIncome: Double,
    cardBreakdowns: List<CardBreakdown> = emptyList()
) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.fillMaxWidth()) {
        // 지출 - 항상 표시 (헤더 오른쪽)
        PaymentHeader(
            emoji = "💳",
            name = strings.card,
            tint = Color(0xFFF9A825),
            trailing = "-${strings.amountWithUnit(Utils.formatAmount(expense))}",
            trailingColor = MaterialTheme.colorScheme.error
        )

        // 실제 사용 / 정산수입 (더치페이 시만 표시)
        val dutchRows = buildList {
            if (actualExpense != expense) {
                add(
                    Triple(
                        strings.actualUsage,
                        "-${strings.amountWithUnit(Utils.formatAmount(actualExpense))}",
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            if (settlementIncome > 0) {
                add(
                    Triple(
                        strings.settlementIncome,
                        "+${strings.amountWithUnit(Utils.formatAmount(settlementIncome))}",
                        MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
        if (dutchRows.isNotEmpty()) {
            PaymentRowsBox(rows = dutchRows)
        }

        // 카드별 내역 (2개 이상의 카드가 사용된 경우만 표시)
        if (cardBreakdowns.size > 1 || (cardBreakdowns.size == 1 && cardBreakdowns.first().cardName != null)) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = strings.cardBreakdown,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            PaymentRowsBox(
                rows = cardBreakdowns.map { breakdown ->
                    Triple(
                        breakdown.cardName ?: strings.unspecifiedCard,
                        "-${strings.amountWithUnit(Utils.formatAmount(breakdown.expense))}",
                        MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    }
}

private enum class StatTab { INCOME, EXPENSE, SAVING, INVESTMENT }

@Composable
private fun StatTabBar(
    tabs: List<StatTab>,
    selectedTab: StatTab,
    onTabSelected: (StatTab) -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        tabs.forEach { tab ->
            SelectablePillChip(
                label = when (tab) {
                    StatTab.INCOME -> strings.income
                    StatTab.EXPENSE -> strings.expense
                    StatTab.SAVING -> strings.saving
                    StatTab.INVESTMENT -> strings.investment
                },
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun OtherPaymentSummaryCard(
    income: Double,
    expense: Double
) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.fillMaxWidth()) {
        PaymentHeader(emoji = "📦", name = strings.other, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        PaymentRowsBox(
            rows = listOf(
                Triple(
                    strings.income,
                    "+${strings.amountWithUnit(Utils.formatAmount(income))}",
                    MaterialTheme.colorScheme.primary
                ),
                Triple(
                    strings.expense,
                    "-${strings.amountWithUnit(Utils.formatAmount(expense))}",
                    MaterialTheme.colorScheme.error
                )
            )
        )
    }
}
