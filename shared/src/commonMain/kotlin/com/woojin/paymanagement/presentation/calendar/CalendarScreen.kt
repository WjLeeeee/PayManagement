package com.woojin.paymanagement.presentation.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji
import com.woojin.paymanagement.presentation.tutorial.CalendarTutorialOverlay
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.Utils
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    tutorialViewModel: com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel,
    onOpenDrawer: () -> Unit = {},
    onDateDetailClick: (LocalDate) -> Unit = {},
    onStatisticsClick: (PayPeriod) -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    onPayPeriodChanged: (PayPeriod) -> Unit = {},
    onParsedTransactionsClick: () -> Unit = {}
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

    var fabExpanded by remember { mutableStateOf(false) }

    // 스와이프 애니메이션을 위한 offset 상태
    var targetOffsetX by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = if (isDragging) {
            // 드래그 중에는 애니메이션 없이 즉시 반영
            tween(durationMillis = 0)
        } else {
            // 드래그 끝난 후에는 부드럽게 애니메이션
            tween(durationMillis = 300)
        },
        label = "swipe_animation"
    )
    var totalDragAmount by remember { mutableStateOf(0f) }

    // 화면 너비 가져오기
    var screenWidth by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .onGloballyPositioned { coordinates ->
                    screenWidth = coordinates.size.width.toFloat()
                }
                .offset(x = with(density) { animatedOffsetX.toDp() })
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDragAmount = 0f
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            // 화면 너비의 30% 이상 드래그했으면 페이지 전환
                            val threshold = screenWidth * 0.3f
                            if (totalDragAmount > threshold) {
                                // 오른쪽으로 스와이프 -> 이전 기간
                                viewModel.navigateToPreviousPeriod()
                            } else if (totalDragAmount < -threshold) {
                                // 왼쪽으로 스와이프 -> 다음 기간
                                viewModel.navigateToNextPeriod()
                            }
                            // 애니메이션으로 원위치
                            targetOffsetX = 0f
                            totalDragAmount = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            // 드래그 취소 시 원위치
                            targetOffsetX = 0f
                            totalDragAmount = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDragAmount += dragAmount
                            // 화면 너비의 30%까지만 따라가도록 제한
                            val maxOffset = screenWidth * 0.3f
                            targetOffsetX = totalDragAmount.coerceIn(-maxOffset, maxOffset)
                        }
                    )
                }
        ) {
            // Drawer Menu Button & Year/Month Header
            if (uiState.currentPayPeriod != null) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "메뉴",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    uiState.selectedDate?.let { selectedDate ->
                        PayPeriodHeader(
                            selectedDate = selectedDate,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            } else {
                // Pay Period가 null인 경우에도 메뉴 버튼 표시
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "메뉴",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (uiState.currentPayPeriod != null) {

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
                    isMoveMode = uiState.isMoveMode,
                    onDateSelected = { date ->
                        if (uiState.isMoveMode) {
                            viewModel.moveTransactionToDate(date)
                        } else {
                            viewModel.selectDate(date)
                        }
                    },
                    tutorialViewModel = tutorialViewModel
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Transaction Display
                DailyTransactionCard(
                    selectedDate = uiState.selectedDate,
                    transactions = uiState.transactions,
                    isMoveMode = uiState.isMoveMode,
                    transactionToMove = uiState.transactionToMove,
                    onTransactionLongClick = { transaction ->
                        viewModel.startMoveMode(transaction)
                    },
                    onCancelMoveMode = {
                        viewModel.cancelMoveMode()
                    },
                    onClick = { date ->
                        if (date != null && !uiState.isMoveMode) {
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

        // Expandable Floating Action Button
        ExpandableFab(
            expanded = fabExpanded,
            onExpandedChange = { fabExpanded = it },
            fabModifier = Modifier
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
            items = listOf(
                FabAction(
                    icon = "📱",
                    label = "카드 결제 내역",
                    onClick = onParsedTransactionsClick
                ),
                FabAction(
                    icon = "➕",
                    label = "거래 추가",
                    onClick = onAddTransactionClick
                )
            )
        )

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
    selectedDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val year = selectedDate.year
    val month = selectedDate.monthNumber

    Text(
        text = "${year}년 ${month}월",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
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
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "급여 기간 요약",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    androidx.compose.material3.IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = if (isMoneyVisible) "금액 숨기기" else "금액 보기",
                            tint = if (isMoneyVisible) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 수입/지출/잔액 표시 (아이콘 추가)
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
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "+${Utils.formatAmount(income)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = if (!isMoneyVisible) Modifier.blur(8.dp) else Modifier
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
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "-${Utils.formatAmount(expense)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = if (!isMoneyVisible) Modifier.blur(8.dp) else Modifier
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
                            }${Utils.formatAmount(kotlin.math.abs(balance))}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                balance > 0 -> MaterialTheme.colorScheme.primary
                                balance < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = if (!isMoneyVisible) Modifier.blur(8.dp) else Modifier
                        )
                    }
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
    isMoveMode: Boolean = false,
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
                val dayOfWeek = date.dayOfWeek.ordinal // 0=Monday, 6=Sunday

                CalendarDay(
                    day = date.dayOfMonth,
                    hasIncome = hasIncome,
                    hasExpense = hasExpense,
                    isSelected = selectedDate == date,
                    isInCurrentPeriod = isInCurrentPeriod,
                    isToday = date == today,
                    dayOfWeek = dayOfWeek,
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
    dayOfWeek: Int, // 0=Monday, 5=Saturday, 6=Sunday
    onClick: () -> Unit
) {
    // 다크모드 확인: onSurface 색상이 밝으면 다크모드
    val isDarkMode = MaterialTheme.colorScheme.onSurface.red > 0.5f

    // 주말 배경색 계산 (토요일: 파랑, 일요일: 빨강)
    val weekendBackground = when (dayOfWeek) {
        5 -> if (isDarkMode) {
            Color(0xFF1565C0).copy(alpha = 0.2f) // 토요일 - 다크모드에서는 어두운 파랑 + 낮은 투명도
        } else {
            Color(0xFFE3F2FD).copy(alpha = 0.5f) // 토요일 - 라이트모드: 연한 파랑
        }
        6 -> if (isDarkMode) {
            Color(0xFFC62828).copy(alpha = 0.2f) // 일요일 - 다크모드에서는 어두운 빨강 + 낮은 투명도
        } else {
            Color(0xFFFFEBEE).copy(alpha = 0.5f) // 일요일 - 라이트모드: 연한 빨강
        }
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() }
            .clip(CircleShape)
            .background(weekendBackground)
            .then(
                when {
                    isToday -> Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    isSelected -> Modifier.border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                fontSize = 14.sp,
                fontWeight = if (hasIncome || hasExpense) FontWeight.Bold else FontWeight.Normal,
                color = if (isInCurrentPeriod) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 인디케이터 점
            if (hasIncome || hasExpense) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (hasIncome) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (hasExpense) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DailyTransactionCard(
    selectedDate: LocalDate?,
    transactions: List<Transaction>,
    isMoveMode: Boolean = false,
    transactionToMove: Transaction? = null,
    onTransactionLongClick: (Transaction) -> Unit = {},
    onCancelMoveMode: () -> Unit = {},
    onClick: (LocalDate?) -> Unit = {},
    tutorialViewModel: com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel? = null
) {
    val dayTransactions = selectedDate?.let { date ->
        transactions.filter { it.date == date }
    } ?: emptyList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 0.dp, max = 170.dp)
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
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
                // 헤더 또는 이동 모드 메시지
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMoveMode) {
                        Text(
                            text = "📍 이동할 날짜를 선택하세요",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "취소",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onCancelMoveMode() }
                                .padding(4.dp)
                        )
                    } else {
                        Text(
                            text = if (selectedDate != null) {
                                val count = dayTransactions.size
                                if (count > 0) {
                                    "📝 ${selectedDate.monthNumber}월 ${selectedDate.dayOfMonth}일 거래 내역 (${count}건)"
                                } else {
                                    "📝 ${selectedDate.monthNumber}월 ${selectedDate.dayOfMonth}일 거래 내역"
                                }
                            } else {
                                "📝 날짜를 선택해서 메모 보기"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (dayTransactions.isNotEmpty()) {
                    // LazyColumn으로 스크롤 가능한 거래 목록 생성
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dayTransactions) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                isSelected = isMoveMode && transaction.id == transactionToMove?.id,
                                onLongClick = { onTransactionLongClick(transaction) }
                            )
                        }
                    }
                } else if (selectedDate != null) {
                    Text(
                        text = "이 날짜에 거래 내역이 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "캘린더에서 날짜를 클릭하여 해당 날짜의 메모를 확인하세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    transaction: Transaction,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .then(
                if (isSelected) {
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.Top
    ) {
        // 거래 유형 표시
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = 8.dp)
                .clip(CircleShape)
                .background(
                    if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = getCategoryEmoji(transaction.category),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"}${
                        Utils.formatAmount(
                            transaction.amount
                        )
                    }원",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (transaction.memo.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.memo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}