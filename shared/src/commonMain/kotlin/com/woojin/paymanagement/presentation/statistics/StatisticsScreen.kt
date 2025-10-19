package com.woojin.paymanagement.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.BalanceCardSummary
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.GiftCardSummary
import com.woojin.paymanagement.data.PaymentMethodSummary
import com.woojin.paymanagement.presentation.components.PieChart
import com.woojin.paymanagement.presentation.calculator.CalculatorDialog
import com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.Utils
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
        // Header with back button and calculator button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }

            Text(
                text = "통계",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = { viewModel.showCalculatorDialog() }) {
                Text(
                    text = "계산기",
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            }
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

        // Income Chart
        statisticsData.chartData?.let { chartData ->
            if (chartData.incomeItems.isNotEmpty()) {
                ChartSection(
                    title = "수입 분석",
                    items = chartData.incomeItems,
                    total = chartData.totalIncome
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Expense Chart
        statisticsData.chartData?.let { chartData ->
            if (chartData.expenseItems.isNotEmpty()) {
                ChartSection(
                    title = "지출 분석",
                    items = chartData.expenseItems,
                    total = chartData.totalExpense
                )
            }
        }

        // Payment Method Summary
        statisticsData.paymentSummary?.let { paymentSummary ->
            if (paymentSummary.cashIncome > 0 || paymentSummary.cashExpense > 0 || paymentSummary.cardExpense > 0 ||
                paymentSummary.balanceCards.isNotEmpty() || paymentSummary.giftCards.isNotEmpty()) {

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "결제 수단별 분석",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                PaymentMethodSection(paymentSummary = paymentSummary)
            }
        }

        if (statisticsData.chartData?.let { it.incomeItems.isEmpty() && it.expenseItems.isEmpty() } == true) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.LightGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "이 기간에 거래 내역이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Calculator Dialog
        if (uiState.showCalculatorDialog) {
            CalculatorDialog(
                transactions = transactions,
                onDismiss = { viewModel.hideCalculatorDialog() }
            )
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF), // 매우 연한 파랑
                            Color(0xFFFFFEF7), // 매우 연한 노랑
                            Color(0xFFFFFAFA)  // 매우 연한 빨강
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPreviousPeriod) {
                    Text("◀", fontSize = 16.sp, color = Color.Black)
                }

                Text(
                    text = currentPayPeriod.displayText,
                    color = Color.Black,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onNextPeriod) {
                    Text("▶", fontSize = 16.sp, color = Color.Black)
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
    val balance = totalIncome - totalExpense

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF), // 매우 연한 파랑
                            Color(0xFFFFFEF7), // 매우 연한 노랑
                            Color(0xFFFFFAFA)  // 매우 연한 빨강
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📊 급여 기간 요약",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
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
                                text = "수입",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Blue
                            )
                        }
                        Text(
                            text = "+${Utils.formatAmount(totalIncome)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
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
                                text = "지출",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                        }
                        Text(
                            text = "-${Utils.formatAmount(totalExpense)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
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
                                text = "잔액",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = "${
                                when {
                                    balance > 0 -> "+"
                                    balance < 0 -> "-"
                                    else -> ""
                                }
                            }${Utils.formatAmount(kotlin.math.abs(balance))}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                balance > 0 -> Color.Blue
                                balance < 0 -> Color.Red
                                else -> Color.Black
                            }
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(4.dp))

        Text(
//            text = "${if (amount > 0 && label != "잔액") "+" else ""}${Utils.formatAmount(amount)}원",
            text = when {
                label == "잔액" && amount > 0 -> "+${Utils.formatAmount(amount)}원"
                label == "잔액" && amount < 0 -> "-${Utils.formatAmount(kotlin.math.abs(amount))}원"
                label == "잔액" -> "${Utils.formatAmount(amount)}원"
                amount > 0 -> "+${Utils.formatAmount(amount)}원"
                amount < 0 -> "-${Utils.formatAmount(amount)}원"
                else -> "${Utils.formatAmount(amount)}원"

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
    total: Double
) {
    // 선택된 카테고리 상태
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // 4% 미만 항목들을 "기타"로 묶기
    val (processedItems, mainItems, smallItems) = remember(items, total) {
        val threshold = 4.0f
        val mainItems = items.filter { it.percentage >= threshold }
        val smallItems = items.filter { it.percentage < threshold }

        if (smallItems.isEmpty()) {
            Triple(items, items, emptyList())
        } else {
            val etcAmount = smallItems.sumOf { it.amount.toDouble() }
            val etcPercentage = smallItems.sumOf { it.percentage.toDouble() }.toFloat()

            val etcItem = com.woojin.paymanagement.data.ChartItem(
                category = "기타",
                amount = etcAmount,
                percentage = etcPercentage,
                color = Color.Gray
            )

            Triple(mainItems + etcItem, mainItems, smallItems)
        }
    }

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PieChart(
                    items = processedItems,
                    chartSize = 120.dp,
                    showLegend = false,
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
                        ChartLegendItem(
                            item = item,
                            isSubItem = false,
                            isSelected = selectedCategory == item.category
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
                                category = "기타",
                                amount = etcTotal,
                                percentage = etcPercentage,
                                color = Color.Gray
                            ),
                            isSubItem = false,
                            isSelected = selectedCategory == "기타"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 기타 내부 항목들 (들여쓰기)
                        smallItems.forEach { item ->
                            ChartLegendItem(
                                item = item,
                                isSubItem = true,
                                isSelected = selectedCategory == item.category
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
    isSelected: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isSubItem) 24.dp else 0.dp)
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
                    color = Color.Gray
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
                        text = getCategoryEmoji(item.category),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = item.category,
                        style = if (isSelected) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else if (isSubItem) FontWeight.Normal else FontWeight.Medium,
                        color = if (isSubItem) Color.DarkGray else Color.Black
                    )
                }
                Text(
                    text = "${Utils.formatAmount(item.amount)}원",
                    style = if (isSelected) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.Black else Color.Gray
                )
            }
        }

        Text(
            text = "${(item.percentage * 10).toInt() / 10.0}%",
            style = if (isSelected) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.ExtraBold else if (isSubItem) FontWeight.Normal else FontWeight.Bold,
            color = if (isSubItem) Color.DarkGray else Color.Black
        )
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
                settlementIncome = paymentSummary.settlementIncome
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Balance Cards Summary
        paymentSummary.balanceCards.forEach { balanceCard ->
            BalanceCardSummaryCard(balanceCard = balanceCard)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Gift Cards Summary
        paymentSummary.giftCards.forEach { giftCard ->
            GiftCardSummaryCard(giftCard = giftCard)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CashSummaryCard(
    income: Double,
    expense: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF), // 매우 연한 파랑
                            Color(0xFFFFFEF7), // 매우 연한 노랑
                            Color(0xFFFFFAFA)  // 매우 연한 빨강
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💰 현금",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "수입",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "+${Utils.formatAmount(income)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                    }

                    Column {
                        Text(
                            text = "지출",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "-${Utils.formatAmount(expense)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }

                    Column {
                        Text(
                            text = "차액",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        val balance = income - expense
                        Text(
                            text = when {
                                balance > 0 -> "+${Utils.formatAmount(balance)}원"
                                balance < 0 -> "-${Utils.formatAmount(kotlin.math.abs(balance))}원"
                                else -> "${Utils.formatAmount(balance)}원"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                balance > 0 -> Color.Blue
                                balance < 0 -> Color.Red
                                else -> Color.Black
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
    settlementIncome: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF), // 매우 연한 파랑
                            Color(0xFFFFFEF7), // 매우 연한 노랑
                            Color(0xFFFFFAFA)  // 매우 연한 빨강
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💳 카드",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 지출 (본인 부담) - 항상 표시
                    Column {
                        Text(
                            text = "지출 (본인 부담)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "-${Utils.formatAmount(expense)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }

                    // 실제 사용 (더치페이 시만 표시, 아니면 빈 공간)
                    Column {
                        if (actualExpense != expense) {
                            Text(
                                text = "실제 사용",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = "-${Utils.formatAmount(actualExpense)}원",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF757575)
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
                                text = "정산수입",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = "+${Utils.formatAmount(settlementIncome)}원",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Blue
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
            }
        }
    }
}

@Composable
private fun BalanceCardSummaryCard(
    balanceCard: BalanceCardSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF), // 매우 연한 파랑
                            Color(0xFFFFFEF7), // 매우 연한 노랑
                            Color(0xFFFFFAFA)  // 매우 연한 빨강
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
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "충전",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "+${Utils.formatAmount(balanceCard.income)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                    }

                    Column {
                        Text(
                            text = "사용",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "-${Utils.formatAmount(balanceCard.expense)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }

                    Column {
                        Text(
                            text = "잔액",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "${Utils.formatAmount(balanceCard.currentBalance)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF), // 매우 연한 파랑
                            Color(0xFFFFFEF7), // 매우 연한 노랑
                            Color(0xFFFFFAFA)  // 매우 연한 빨강
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
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "구매",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "+${Utils.formatAmount(giftCard.income)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                    }

                    Column {
                        Text(
                            text = "사용",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "-${Utils.formatAmount(giftCard.expense)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }

                    Column {
                        Text(
                            text = "잔액",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "${Utils.formatAmount(giftCard.currentBalance)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}