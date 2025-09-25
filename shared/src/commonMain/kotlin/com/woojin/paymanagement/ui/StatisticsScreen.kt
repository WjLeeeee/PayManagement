package com.woojin.paymanagement.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.woojin.paymanagement.ui.components.PieChart
import com.woojin.paymanagement.presentation.calculator.CalculatorDialog
import com.woojin.paymanagement.presentation.statistics.StatisticsViewModel
import com.woojin.paymanagement.presentation.statistics.StatisticsUiState
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
    var uiState by remember { mutableStateOf(StatisticsUiState()) }

    LaunchedEffect(initialPayPeriod, availableBalanceCards, availableGiftCards) {
        viewModel.initializeStatistics(initialPayPeriod, availableBalanceCards, availableGiftCards)

        viewModel.getStatisticsFlow(availableBalanceCards, availableGiftCards)
            .collectLatest { newState ->
                uiState = newState
            }
    }

    LaunchedEffect(viewModel.uiState.currentPayPeriod) {
        if (viewModel.uiState.currentPayPeriod != null) {
            viewModel.getStatisticsFlow(availableBalanceCards, availableGiftCards)
                .collectLatest { newState ->
                    uiState = newState
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
        uiState.chartData?.let { chartData ->
            SummaryCard(
                totalIncome = chartData.totalIncome,
                totalExpense = chartData.totalExpense
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Income Chart
        uiState.chartData?.let { chartData ->
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
        uiState.chartData?.let { chartData ->
            if (chartData.expenseItems.isNotEmpty()) {
                ChartSection(
                    title = "지출 분석",
                    items = chartData.expenseItems,
                    total = chartData.totalExpense
                )
            }
        }

        // Payment Method Summary
        uiState.paymentSummary?.let { paymentSummary ->
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

        if (uiState.chartData?.let { it.incomeItems.isEmpty() && it.expenseItems.isEmpty() } == true) {
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
        colors = CardDefaults.cardColors(containerColor = Color.LightGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPreviousPeriod) {
                Text("◀", fontSize = 20.sp, color = Color.Black)
            }
            
            Text(
                text = currentPayPeriod.displayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            
            TextButton(onClick = onNextPeriod) {
                Text("▶", fontSize = 20.sp, color = Color.Black)
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
        colors = CardDefaults.cardColors(containerColor = Color.LightGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "급여 기간 요약",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "총 수입",
                    amount = totalIncome,
                    color = Color.Blue
                )
                
                SummaryItem(
                    label = "총 지출",
                    amount = totalExpense,
                    color = Color.Red
                )
                
                SummaryItem(
                    label = "잔액",
                    amount = balance,
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

        println("label: $label, amount: ${amount}, color: $color")
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
                    items = items,
                    chartSize = 180.dp,
                    showLegend = true
                )
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
        colors = CardDefaults.cardColors(containerColor = Color.LightGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "💰 현금",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
                        text = "${if (balance > 0) "+" else ""}${Utils.formatAmount(balance)}원",
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

@Composable
private fun CardSummaryCard(
    expense: Double,
    actualExpense: Double,
    settlementIncome: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "💳 카드",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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

@Composable
private fun BalanceCardSummaryCard(
    balanceCard: BalanceCardSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🎫 ${balanceCard.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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

@Composable
private fun GiftCardSummaryCard(
    giftCard: GiftCardSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🎁 ${giftCard.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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