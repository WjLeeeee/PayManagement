package com.woojin.paymanagement.ui

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.PayPeriodCalculator
import com.woojin.paymanagement.utils.PreferencesManager
import com.woojin.paymanagement.utils.Utils
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

@Composable
fun CalendarScreen(
    transactions: List<Transaction> = emptyList(),
    selectedDate: LocalDate? = null,
    initialPayPeriod: PayPeriod? = null,
    onDateSelected: (LocalDate) -> Unit = {},
    onDateDetailClick: (LocalDate) -> Unit = {},
    onStatisticsClick: (PayPeriod) -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    onPayPeriodChanged: (PayPeriod) -> Unit = {},
    preferencesManager: PreferencesManager
) {
    // 월급 기준으로 현재 기간 계산
    val payday = preferencesManager.getPayday()
    val adjustment = preferencesManager.getPaydayAdjustment()
    
    var currentPayPeriod by remember {
        mutableStateOf(
            initialPayPeriod ?: PayPeriodCalculator.getCurrentPayPeriod(payday, adjustment)
        )
    }

    // 급여 기간 변경 시 App.kt에 알림
    LaunchedEffect(currentPayPeriod) {
        onPayPeriodChanged(currentPayPeriod)
    }
    
    // 초기 선택 날짜 설정 (첫 로드 시에만)
    var internalSelectedDate by remember {
        mutableStateOf(
            selectedDate ?: PayPeriodCalculator.getRecommendedDateForPeriod(currentPayPeriod, payday, adjustment)
        )
    }
    
    // 외부에서 selectedDate가 변경되면 내부 상태도 업데이트
    LaunchedEffect(selectedDate) {
        selectedDate?.let { 
            internalSelectedDate = it 
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Pay Period Navigation
            PayPeriodHeader(
                currentPayPeriod = currentPayPeriod,
                onPreviousPeriod = {
                    currentPayPeriod = PayPeriodCalculator.getPreviousPayPeriod(currentPayPeriod, payday, adjustment)
                    // 새로운 기간에 맞는 날짜로 조정
                    val newSelectedDate = PayPeriodCalculator.getRecommendedDateForPeriod(currentPayPeriod, payday, adjustment)
                    internalSelectedDate = newSelectedDate
                    onDateSelected(newSelectedDate)
                },
                onNextPeriod = {
                    currentPayPeriod = PayPeriodCalculator.getNextPayPeriod(currentPayPeriod, payday, adjustment)
                    // 새로운 기간에 맞는 날짜로 조정
                    val newSelectedDate = PayPeriodCalculator.getRecommendedDateForPeriod(currentPayPeriod, payday, adjustment)
                    internalSelectedDate = newSelectedDate
                    onDateSelected(newSelectedDate)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pay Period Summary
            PayPeriodSummaryCard(
                transactions = transactions,
                payPeriod = currentPayPeriod,
                onStatisticsClick = { onStatisticsClick(currentPayPeriod) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar Grid
            CalendarGrid(
                payPeriod = currentPayPeriod,
                transactions = transactions,
                selectedDate = internalSelectedDate,
                onDateSelected = { date ->
                    internalSelectedDate = date
                    onDateSelected(date)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Daily Transaction Display
            DailyTransactionCard(
                selectedDate = internalSelectedDate,
                transactions = transactions,
                onClick = { date ->
                    if (date != null) {
                        onDateDetailClick(date)
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddTransactionClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color.LightGray,
            shape = RoundedCornerShape(12.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "거래 내역 추가",
                tint = Color.Black
            )
        }
    }
}

@Composable
private fun PayPeriodHeader(
    currentPayPeriod: PayPeriod,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPreviousPeriod) {
            Text("◀", fontSize = 20.sp, color = Color.Black)
        }
        
        Text(
            text = currentPayPeriod.displayText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        
        TextButton(onClick = onNextPeriod) {
            Text("▶", fontSize = 20.sp, color = Color.Black)
        }
    }
}

@Composable
private fun PayPeriodSummaryCard(
    transactions: List<Transaction>,
    payPeriod: PayPeriod,
    onStatisticsClick: () -> Unit = {}
) {
    val periodTransactions = transactions.filter { transaction ->
        transaction.date >= payPeriod.startDate && transaction.date <= payPeriod.endDate
    }
    
    val income = periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expense = periodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val balance = income - expense
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStatisticsClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.LightGray
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "급여 기간 요약",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("수입", color = Color.Blue)
                    Text(
                        text = "+${Utils.formatAmount(income)}원",
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                }
                
                Column {
                    Text("지출", color = Color.Red)
                    Text(
                        text = "-${Utils.formatAmount(expense)}원",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
                
                Column {
                    Text("잔액")
                    Text(
                        text = "${when {
                            balance > 0 -> "+"
                            else -> ""
                        }}${Utils.formatAmount(balance)}원",
                        fontWeight = FontWeight.Bold,
                        color = when {
                            balance > 0 -> Color.Blue
                            balance < 0 -> Color.Red
                            else -> Color.Black
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    payPeriod: PayPeriod,
    transactions: List<Transaction>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    // 오늘 날짜 계산
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // 월급 기간에 포함되는 모든 날짜 계산
    val allDates = generateDateSequence(payPeriod.startDate, payPeriod.endDate)

    // 첫 번째 달의 첫 주 계산
    val firstDayOfFirstMonth = LocalDate(payPeriod.startDate.year, payPeriod.startDate.month, 1)
    val firstDayOfWeek = (firstDayOfFirstMonth.dayOfWeek.ordinal + 1) % 7
    
    Column {
        // Week Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar Days
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
//            modifier = Modifier.height(300.dp)
        ) {
            // Empty cells for days before first date starts
            val emptyDaysAtStart = (payPeriod.startDate.dayOfMonth - 1 + firstDayOfWeek) % 7
            items(emptyDaysAtStart) {
                Box(modifier = Modifier.height(40.dp))
            }
            
            // All dates in pay period
            items(allDates) { date ->
                val dayTransactions = transactions.filter { it.date == date }
                val hasIncome = dayTransactions.any { it.type == TransactionType.INCOME }
                val hasExpense = dayTransactions.any { it.type == TransactionType.EXPENSE }
                val isInCurrentPeriod = date >= payPeriod.startDate && date <= payPeriod.endDate
                
                CalendarDay(
                    day = date.dayOfMonth,
                    hasIncome = hasIncome,
                    hasExpense = hasExpense,
                    isSelected = selectedDate == date,
                    isInCurrentPeriod = isInCurrentPeriod,
                    isToday = date == today,
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

// 날짜 시퀀스 생성 함수
private fun generateDateSequence(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var currentDate = startDate
    while (currentDate <= endDate) {
        dates.add(currentDate)
        currentDate = currentDate.plus(1, DateTimeUnit.DAY)
    }
    return dates
}

@Composable
private fun CalendarDay(
    day: Int,
    hasIncome: Boolean,
    hasExpense: Boolean,
    isSelected: Boolean,
    isInCurrentPeriod: Boolean = true,
    isToday: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() }
            .clip(CircleShape)
            .background(
                when {
                    hasIncome && hasExpense -> Color.Yellow.copy(alpha = 0.3f)
                    hasIncome -> Color.Blue.copy(alpha = 0.3f)
                    hasExpense -> Color.Red.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .then(
                when {
                    isToday -> Modifier.border(2.dp, Color.Black, CircleShape)
                    isSelected -> Modifier.border(2.dp, Color.Gray, CircleShape)
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            fontSize = 14.sp,
            fontWeight = if (hasIncome || hasExpense) FontWeight.Bold else FontWeight.Normal,
            color = if (isInCurrentPeriod) Color.Black else Color.Gray
        )
    }
}

@Composable
private fun DailyTransactionCard(
    selectedDate: LocalDate?,
    transactions: List<Transaction>,
    onClick: (LocalDate?) -> Unit = {}
) {
    val dayTransactions = selectedDate?.let { date ->
        transactions.filter { it.date == date }
    } ?: emptyList()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(selectedDate) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.LightGray
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (selectedDate != null) {
                    "${selectedDate.monthNumber}월 ${selectedDate.dayOfMonth}일 거래 내역"
                } else {
                    "날짜를 선택해서 메모 보기"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (dayTransactions.isNotEmpty()) {
                dayTransactions.forEach { transaction ->
                    TransactionItem(transaction)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (selectedDate != null) {
                Text(
                    text = "이 날짜에 거래 내역이 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            } else {
                Text(
                    text = "캘린더에서 날짜를 클릭하여 해당 날짜의 메모를 확인하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // 거래 유형 표시
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = 8.dp)
                .clip(CircleShape)
                .background(
                    if (transaction.type == TransactionType.INCOME) Color.Blue else Color.Red
                )
                .align(Alignment.Top)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"}${Utils.formatAmount(transaction.amount)}원",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (transaction.type == TransactionType.INCOME) Color.Blue else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }

            // 결제 수단 표시
            val paymentMethodText = when (transaction.type) {
                TransactionType.INCOME -> {
                    when (transaction.incomeType) {
                        com.woojin.paymanagement.data.IncomeType.CASH -> "현금"
                        com.woojin.paymanagement.data.IncomeType.BALANCE_CARD -> "잔액권 ${transaction.cardName ?: ""}"
                        com.woojin.paymanagement.data.IncomeType.GIFT_CARD -> "상품권 ${transaction.cardName ?: ""}"
                        null -> "현금"
                    }
                }
                TransactionType.EXPENSE -> {
                    when (transaction.paymentMethod) {
                        com.woojin.paymanagement.data.PaymentMethod.CASH -> "현금"
                        com.woojin.paymanagement.data.PaymentMethod.BALANCE_CARD -> "잔액권 ${transaction.cardName ?: ""}"
                        com.woojin.paymanagement.data.PaymentMethod.GIFT_CARD -> "상품권 ${transaction.cardName ?: ""}"
                        null -> "현금"
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = paymentMethodText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            if (transaction.memo.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.memo,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }
    }
}