package com.woojin.paymanagement.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.presentation.tutorial.CalendarTutorialOverlay
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.Utils
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    tutorialViewModel: com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel,
    onDateDetailClick: (LocalDate) -> Unit = {},
    onStatisticsClick: (PayPeriod) -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    onPayPeriodChanged: (PayPeriod) -> Unit = {}
) {
    val uiState = viewModel.uiState
    val tutorialUiState = tutorialViewModel.uiState

    // 네비게이션바 높이 계산
    val density = LocalDensity.current
    val navigationBarHeight = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    // 급여 기간 변경 시 App.kt에 알림
    LaunchedEffect(uiState.currentPayPeriod) {
        uiState.currentPayPeriod?.let { payPeriod ->
            onPayPeriodChanged(payPeriod)
        }
    }

    // EdgeToEdge 대응은 CalendarTutorialOverlay에서 처리됩니다

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Pay Period Navigation
            if (uiState.currentPayPeriod != null) {
                PayPeriodHeader(
                    currentPayPeriod = uiState.currentPayPeriod,
                    onPreviousPeriod = { viewModel.navigateToPreviousPeriod() },
                    onNextPeriod = { viewModel.navigateToNextPeriod() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pay Period Summary
                PayPeriodSummaryCard(
                    transactions = uiState.transactions,
                    payPeriod = uiState.currentPayPeriod,
                    isMoneyVisible = uiState.isMoneyVisible,
                    onToggleVisibility = { viewModel.toggleMoneyVisibility() },
                    onStatisticsClick = { onStatisticsClick(uiState.currentPayPeriod) },
                    tutorialViewModel = tutorialViewModel
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Calendar Grid
                CalendarGrid(
                    payPeriod = uiState.currentPayPeriod,
                    transactions = uiState.transactions,
                    selectedDate = uiState.selectedDate,
                    onDateSelected = { date -> viewModel.selectDate(date) },
                    tutorialViewModel = tutorialViewModel
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Transaction Display
                DailyTransactionCard(
                    selectedDate = uiState.selectedDate,
                    transactions = uiState.transactions,
                    onClick = { date ->
                        if (date != null) {
                            onDateDetailClick(date)
                        }
                    },
                    tutorialViewModel = tutorialViewModel
                )
            } else {
                // 로딩 상태 표시
                Text(
                    text = "로딩 중...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddTransactionClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + navigationBarHeight
                )
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    tutorialViewModel.updateTargetBounds(
                        "floating_action_button",
                        bounds
                    )
                },
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

        // Tutorial Overlay
        if (tutorialUiState.shouldShowTutorial && tutorialUiState.currentStep != null) {
            CalendarTutorialOverlay(
                currentStep = tutorialUiState.currentStep,
                totalSteps = tutorialUiState.steps.size,
                currentStepIndex = tutorialUiState.currentStepIndex,
                onNext = tutorialViewModel::nextStep,
                onSkip = tutorialViewModel::skipTutorial,
                onComplete = tutorialViewModel::completeTutorial,
                calendarGridBounds = tutorialViewModel.getTargetBounds("calendar_grid")
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
            Text("◀", fontSize = 16.sp, color = Color.Black)
        }

        Text(
            text = currentPayPeriod.displayText,
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

@Composable
private fun PayPeriodSummaryCard(
    transactions: List<Transaction>,
    payPeriod: PayPeriod,
    isMoneyVisible: Boolean,
    onToggleVisibility: () -> Unit,
    onStatisticsClick: () -> Unit = {},
    tutorialViewModel: com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel? = null
) {
    val periodTransactions = transactions.filter { transaction ->
        transaction.date >= payPeriod.startDate && transaction.date <= payPeriod.endDate
    }

    val income = periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expense =
        periodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val balance = income - expense

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStatisticsClick() }
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                tutorialViewModel?.updateTargetBounds(
                    "pay_period_summary",
                    bounds
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.LightGray
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "급여 기간 요약",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                androidx.compose.material3.IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = if (isMoneyVisible) "금액 숨기기" else "금액 보기",
                        tint = if (isMoneyVisible) Color.Black.copy(alpha = 0.3f) else Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

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
                        color = Color.Blue,
                        modifier = if (!isMoneyVisible) Modifier.blur(8.dp) else Modifier
                    )
                }

                Column {
                    Text("지출", color = Color.Red)
                    Text(
                        text = "-${Utils.formatAmount(expense)}원",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        modifier = if (!isMoneyVisible) Modifier.blur(8.dp) else Modifier
                    )
                }

                Column {
                    Text("잔액")
                    Text(
                        text = "${
                            when {
                                balance > 0 -> "+"
                                balance < 0 -> "-"
                                else -> ""
                            }
                        }${Utils.formatAmount(balance)}원",
                        fontWeight = FontWeight.Bold,
                        color = when {
                            balance > 0 -> Color.Blue
                            balance < 0 -> Color.Red
                            else -> Color.Black
                        },
                        modifier = if (!isMoneyVisible) Modifier.blur(8.dp) else Modifier
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
    onDateSelected: (LocalDate) -> Unit,
    tutorialViewModel: com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel? = null
) {
    // 오늘 날짜 계산
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // 월급 기간에 포함되는 모든 날짜 계산
    val allDates = generateDateSequence(payPeriod.startDate, payPeriod.endDate)

    // 첫 번째 달의 첫 주 계산
    val firstDayOfFirstMonth = LocalDate(payPeriod.startDate.year, payPeriod.startDate.month, 1)
    val firstDayOfWeek = (firstDayOfFirstMonth.dayOfWeek.ordinal + 1) % 7

    Column(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            tutorialViewModel?.updateTargetBounds(
                "calendar_grid",
                bounds
            )
        }
    ) {
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
    onClick: (LocalDate?) -> Unit = {},
    tutorialViewModel: com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel? = null
) {
    val dayTransactions = selectedDate?.let { date ->
        transactions.filter { it.date == date }
    } ?: emptyList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 0.dp, max = 200.dp)
            .clickable { onClick(selectedDate) }
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                tutorialViewModel?.updateTargetBounds(
                    "transaction_card",
                    bounds
                )
            },
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
                // LazyColumn으로 스크롤 가능한 거래 목록 생성
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dayTransactions) { transaction ->
                        TransactionItem(transaction)
                    }
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
                    text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"}${
                        Utils.formatAmount(
                            transaction.amount
                        )
                    }원",
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
                        com.woojin.paymanagement.data.PaymentMethod.CARD -> "카드"
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