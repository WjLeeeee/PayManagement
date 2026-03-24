package com.woojin.paymanagement.presentation.statistics

import androidx.compose.foundation.background
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
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
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Period Navigation
        uiState.currentPayPeriod?.let { currentPayPeriod ->
            PayPeriodNavigationCard(
                currentPayPeriod = currentPayPeriod,
                onPreviousPeriod = { viewModel.moveToPreviousPeriod() },
                onNextPeriod = { viewModel.moveToNextPeriod() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Card
        statisticsData.chartData?.let { chartData ->
            SummaryCard(
                totalIncome = chartData.totalIncome,
                totalExpense = chartData.totalExpense
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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

        if (investmentAmount > 0 || lossCutAmount > 0 || profitAmount > 0 || dividendAmount > 0) {
            InvestmentSummaryCard(
                investmentAmount = investmentAmount,
                lossCutAmount = lossCutAmount,
                profitAmount = profitAmount,
                dividendAmount = dividendAmount
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Income Chart
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Expense Chart
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Investment Activity Chart (moved after Expense Chart)
        if (investmentAmount > 0 || lossCutAmount > 0 || profitAmount > 0 || dividendAmount > 0) {
            statisticsData.chartData?.let { chartData ->
                if (chartData.investmentItems.isNotEmpty()) {
                    ChartSection(
                        title = strings.investmentActivityAnalysis,
                        items = chartData.investmentItems,
                        total = chartData.totalInvestment,
                        availableCategories = uiState.availableCategories,
                        transactions = statisticsData.transactions,
                        transactionType = TransactionType.INCOME, // 투자는 수입/지출 혼합 (더미값)
                        groupSmallItems = false, // 투자 활동은 기타로 묶지 않고 모두 표시
                        filterByType = false // 투자 활동은 타입 필터링 하지 않음 (수입/지출 모두 포함)
                    )
                }
            }
        }

        // Saving Activity Chart
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

        // Payment Method Summary
        statisticsData.paymentSummary?.let { paymentSummary ->
            if (paymentSummary.cashIncome > 0 || paymentSummary.cashExpense > 0 || paymentSummary.cardExpense > 0 ||
                paymentSummary.balanceCards.isNotEmpty() || paymentSummary.giftCards.isNotEmpty()) {

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = strings.paymentMethodAnalysis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

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

@Composable
private fun PayPeriodNavigationCard(
    currentPayPeriod: PayPeriod,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit
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

                Text(
                    text = currentPayPeriod.displayText,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onNextPeriod) {
                    Text("▶", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalIncome: Double,
    totalExpense: Double
) {
    val strings = LocalStrings.current
    val balance = totalIncome - totalExpense

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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📊 ${strings.payPeriodSummary}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 수입
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💰",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = strings.income,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "+${strings.amountWithUnit(Utils.formatAmount(totalIncome))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 지출
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💸",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = strings.expense,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "-${strings.amountWithUnit(Utils.formatAmount(totalExpense))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // 잔액
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💵",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = strings.balance,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${
                                when {
                                    balance > 0 -> "+"
                                    balance < 0 -> "-"
                                    else -> ""
                                }
                            }${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(balance)))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                balance > 0 -> MaterialTheme.colorScheme.primary
                                balance < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentSummaryCard(
    investmentAmount: Double,
    lossCutAmount: Double,
    profitAmount: Double,
    dividendAmount: Double
) {
    val strings = LocalStrings.current
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📈 ${strings.investmentActivitySummary}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 한 줄로 표시
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 투자
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "💹 ${strings.investment}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = strings.amountWithUnit(Utils.formatAmount(investmentAmount)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 손절
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "📉 ${strings.stopLoss}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "-${strings.amountWithUnit(Utils.formatAmount(lossCutAmount))}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // 익절
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "📈 ${strings.profitTaking}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "+${strings.amountWithUnit(Utils.formatAmount(profitAmount))}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 배당금
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "💰 ${strings.dividend}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "+${strings.amountWithUnit(Utils.formatAmount(dividendAmount))}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    amount: Double,
    color: Color
) {
    val strings = LocalStrings.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when {
                label == strings.balance && amount > 0 -> "+${strings.amountWithUnit(Utils.formatAmount(amount))}"
                label == strings.balance && amount < 0 -> "-${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(amount)))}"
                label == strings.balance -> strings.amountWithUnit(Utils.formatAmount(amount))
                amount > 0 -> "+${strings.amountWithUnit(Utils.formatAmount(amount))}"
                amount < 0 -> "-${strings.amountWithUnit(Utils.formatAmount(amount))}"
                else -> strings.amountWithUnit(Utils.formatAmount(amount))

            },
            style = MaterialTheme.typography.titleMedium,
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
    // 선택된 카테고리 상태
    var selectedCategory by remember { mutableStateOf<String?>(null) }

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
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                Spacer(modifier = Modifier.height(16.dp))

                // Legend: 주요 항목 + 기타(소항목들)
                Column(
                    modifier = Modifier.fillMaxWidth()
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
                        Spacer(modifier = Modifier.height(8.dp))
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

                        Spacer(modifier = Modifier.height(8.dp))

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
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
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
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (isSubItem) 24.dp else 0.dp)
                .clickable { onClick() }
                .background(
                    color = if (isSelected) item.color.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(vertical = if (isSelected) 8.dp else 0.dp, horizontal = if (isSelected) 8.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (isSubItem) {
                // 서브 아이템은 "ㄴ" 표시
                Text(
                    text = "ㄴ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(if (isSelected) 20.dp else 16.dp)
                    .clip(CircleShape)
                    .background(item.color)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = getCategoryEmoji(item.category, availableCategories),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = item.category,
                        style = if (isSelected) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else if (isSubItem) FontWeight.Normal else FontWeight.Medium,
                        color = if (isSubItem) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = strings.amountWithUnit(Utils.formatAmount(item.amount)),
                    style = if (isSelected) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

            Text(
                text = "${(item.percentage * 10).toInt() / 10.0}%",
                style = if (isSelected) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else if (isSubItem) FontWeight.Normal else FontWeight.Bold,
                color = if (isSubItem) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }

        // 거래 내역 확장 표시
        androidx.compose.animation.AnimatedVisibility(
            visible = isSelected && transactions.isNotEmpty()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (isSubItem) 48.dp else 24.dp, top = 8.dp, end = 8.dp, bottom = 4.dp)
                    .background(
                        color = item.color.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                transactions.forEach { transaction ->
                    val dateText = "${transaction.date.monthNumber}/${transaction.date.dayOfMonth.toString().padStart(2, '0')}"
                    val amountText = Utils.formatAmount(transaction.displayAmount)

                    // 실제 거래 타입에 따라 표시 (transactionType 파라미터가 아닌 transaction.type 사용)
                    when (transaction.type) {
                        TransactionType.INCOME -> {
                            // 수입: 날짜 + 메모 (있으면) + 금액
                            val displayText = if (transaction.memo.isNotBlank()) {
                                "• $dateText - ${transaction.memo} (${strings.amountWithUnit(amountText)})"
                            } else {
                                "• $dateText (${strings.amountWithUnit(amountText)})"
                            }
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TransactionType.EXPENSE -> {
                            // 지출: 날짜 + 사용처 + 메모 (있으면) + 금액
                            val merchant = transaction.merchant ?: ""
                            val displayText = if (transaction.memo.isNotBlank()) {
                                "• $dateText - $merchant (${transaction.memo}) (${strings.amountWithUnit(amountText)})"
                            } else {
                                "• $dateText - $merchant (${strings.amountWithUnit(amountText)})"
                            }
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TransactionType.SAVING -> {
                            // 저축: 날짜 + 메모 (있으면) + 금액
                            val displayText = if (transaction.memo.isNotBlank()) {
                                "• $dateText - ${transaction.memo} (${strings.amountWithUnit(amountText)})"
                            } else {
                                "• $dateText (${strings.amountWithUnit(amountText)})"
                            }
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (transaction != transactions.last()) {
                        Spacer(modifier = Modifier.height(4.dp))
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

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Card Summary
        if (paymentSummary.cardExpense > 0) {
            CardSummaryCard(
                expense = paymentSummary.cardExpense,
                actualExpense = paymentSummary.cardActualExpense,
                settlementIncome = paymentSummary.settlementIncome,
                cardBreakdowns = paymentSummary.cardBreakdowns
            )

            Spacer(modifier = Modifier.height(16.dp))
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

@Composable
private fun CashSummaryCard(
    income: Double,
    expense: Double
) {
    val strings = LocalStrings.current
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💰 ${strings.cash}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.income,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+${strings.amountWithUnit(Utils.formatAmount(income))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = strings.expense,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${strings.amountWithUnit(Utils.formatAmount(expense))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Column {
                        Text(
                            text = strings.differenceAmount,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val balance = income - expense
                        Text(
                            text = when {
                                balance > 0 -> "+${strings.amountWithUnit(Utils.formatAmount(balance))}"
                                balance < 0 -> "-${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(balance)))}"
                                else -> strings.amountWithUnit(Utils.formatAmount(balance))
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                balance > 0 -> MaterialTheme.colorScheme.primary
                                balance < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💳 ${strings.card}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 지출 - 항상 표시
                    Column {
                        Text(
                            text = strings.expense,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${strings.amountWithUnit(Utils.formatAmount(expense))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // 실제 사용 (더치페이 시만 표시, 아니면 빈 공간)
                    Column {
                        if (actualExpense != expense) {
                            Text(
                                text = strings.actualUsage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "-${strings.amountWithUnit(Utils.formatAmount(actualExpense))}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // 빈 공간으로 레이아웃 유지
                            Text(
                                text = "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    // 정산수입 (더치페이 시만 표시, 아니면 빈 공간)
                    Column {
                        if (settlementIncome > 0) {
                            Text(
                                text = strings.settlementIncome,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+${strings.amountWithUnit(Utils.formatAmount(settlementIncome))}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            // 빈 공간으로 레이아웃 유지
                            Text(
                                text = "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }

                // 카드별 내역 (2개 이상의 카드가 사용된 경우만 표시)
                if (cardBreakdowns.size > 1 || (cardBreakdowns.size == 1 && cardBreakdowns.first().cardName != null)) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = strings.cardBreakdown,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    cardBreakdowns.forEach { breakdown ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = breakdown.cardName ?: strings.unspecifiedCard,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "-${strings.amountWithUnit(Utils.formatAmount(breakdown.expense))}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCardSummaryCard(
    balanceCard: BalanceCardSummary
) {
    val strings = LocalStrings.current
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🎫 ${balanceCard.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.charge,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+${strings.amountWithUnit(Utils.formatAmount(balanceCard.income))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = strings.usage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${strings.amountWithUnit(Utils.formatAmount(balanceCard.expense))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Column {
                        Text(
                            text = strings.balance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = strings.amountWithUnit(Utils.formatAmount(balanceCard.currentBalance)),
                            style = MaterialTheme.typography.titleSmall,
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
private fun GiftCardSummaryCard(
    giftCard: GiftCardSummary
) {
    val strings = LocalStrings.current
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🎁 ${giftCard.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.purchase,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+${strings.amountWithUnit(Utils.formatAmount(giftCard.income))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = strings.usage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${strings.amountWithUnit(Utils.formatAmount(giftCard.expense))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Column {
                        Text(
                            text = strings.balance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = strings.amountWithUnit(Utils.formatAmount(giftCard.currentBalance)),
                            style = MaterialTheme.typography.titleSmall,
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
private fun OtherPaymentSummaryCard(
    income: Double,
    expense: Double
) {
    val strings = LocalStrings.current
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📦 ${strings.other}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.income,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+${strings.amountWithUnit(Utils.formatAmount(income))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = strings.expense,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${strings.amountWithUnit(Utils.formatAmount(expense))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}