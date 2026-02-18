package com.woojin.paymanagement.presentation.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.Utils
import kotlinx.coroutines.delay
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
    preferencesManager: com.woojin.paymanagement.utils.PreferencesManager,
    notificationPermissionChecker: com.woojin.paymanagement.utils.NotificationPermissionChecker? = null,
    interstitialAdManager: com.woojin.paymanagement.utils.InterstitialAdManager? = null,
    onOpenDrawer: () -> Unit = {},
    onDateDetailClick: (LocalDate) -> Unit = {},
    onStatisticsClick: (PayPeriod) -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    onPayPeriodChanged: (PayPeriod) -> Unit = {},
    onParsedTransactionsClick: () -> Unit = {},
    onAppExit: () -> Unit = {},
    onRequestPostNotificationPermission: ((onPermissionResult: (Boolean) -> Unit) -> Unit)? = null,
    permissionGuideImage: @Composable (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    val uiState = viewModel.uiState
    val tutorialUiState = tutorialViewModel.uiState

    // 뒤로가기 핸들링을 위한 상태
    var showExitDialog by remember { mutableStateOf(false) }

    // 권한 안내 다이얼로그 상태
    var showPermissionGuideDialog by remember { mutableStateOf(false) }

    // 튜토리얼 완료 후 권한 안내 다이얼로그 표시
    LaunchedEffect(tutorialUiState.shouldShowTutorial) {
        // 튜토리얼이 완료되었고, 권한 안내를 아직 보여주지 않았다면
        if (!tutorialUiState.shouldShowTutorial &&
            preferencesManager.isCalendarTutorialCompleted() &&
            !preferencesManager.isPermissionGuideShown() &&
            notificationPermissionChecker != null
        ) {
            // 약간의 딜레이 후 다이얼로그 표시
            kotlinx.coroutines.delay(500)
            showPermissionGuideDialog = true
        }
    }

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

    // 전면광고 미리 로드
    LaunchedEffect(interstitialAdManager) {
        interstitialAdManager?.loadAd()
    }

    // 뒤로가기 핸들링 - 앱 종료 시 전면광고 표시
    com.woojin.paymanagement.utils.BackHandler {
        showExitDialog = true
    }

    // EdgeToEdge 대응은 CalendarTutorialOverlay에서 처리됩니다

    var fabExpanded by remember { mutableStateOf(false) }
    var showYearMonthPicker by remember { mutableStateOf(false) }

    // HorizontalPager 상태 (무한 스크롤을 위해 큰 pageCount 사용)
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE }
    )

    // 이전 페이지 추적
    var previousPage by remember { mutableStateOf(initialPage) }
    var isInitialized by remember { mutableStateOf(false) }

    // 페이지 변경 감지 및 ViewModel 업데이트
    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage

        if (!isInitialized) {
            // 첫 로드 시에는 navigate 호출 안 함
            isInitialized = true
            previousPage = currentPage
            return@LaunchedEffect
        }

        if (currentPage > previousPage) {
            // 다음 기간으로 이동
            viewModel.navigateToNextPeriod()
        } else if (currentPage < previousPage) {
            // 이전 기간으로 이동
            viewModel.navigateToPreviousPeriod()
        }

        previousPage = currentPage
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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
                            contentDescription = strings.openMenu,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    uiState.selectedDate?.let { selectedDate ->
                        PayPeriodHeader(
                            selectedDate = selectedDate,
                            onClick = { showYearMonthPicker = true },
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
                            contentDescription = strings.openMenu,
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
                    holidays = uiState.holidays,
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
                    holidayNames = uiState.holidayNames,
                    isMoveMode = uiState.isMoveMode,
                    transactionToMove = uiState.transactionToMove,
                    availableCategories = uiState.availableCategories,
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
                // 로딩 상태 표시 - Shimmer Effect
                CalendarScreenShimmer()
            }

            Spacer(modifier = Modifier.weight(1f))
            }
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
                    label = strings.cardPaymentHistory,
                    onClick = onParsedTransactionsClick
                ),
                FabAction(
                    icon = "➕",
                    label = strings.addTransaction,
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

    // 년/월 선택 다이얼로그
    if (showYearMonthPicker && uiState.selectedDate != null) {
        YearMonthPickerDialog(
            currentYear = uiState.selectedDate.year,
            currentMonth = uiState.selectedDate.monthNumber,
            onDismiss = { showYearMonthPicker = false },
            onConfirm = { year, month ->
                viewModel.navigateToYearMonth(year, month)
            }
        )
    }

    // 앱 종료 확인 다이얼로그
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(strings.appExit) },
            text = { Text(strings.exitConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        // 광고 제거가 활성화되어 있으면 광고 없이 바로 종료
                        if (preferencesManager.isAdRemovalActive()) {
                            onAppExit()
                        } else {
                            // 전면광고 표시 후 앱 종료
                            interstitialAdManager?.showAd {
                                // 광고 닫힌 후 앱 종료 콜백 호출
                                onAppExit()
                            }
                        }
                    }
                ) {
                    Text(strings.exitConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // 권한 안내 다이얼로그
    if (showPermissionGuideDialog && notificationPermissionChecker != null) {
        AlertDialog(
            onDismissRequest = {
                // 바깥 클릭 시 닫지 않음 (닫기 버튼으로만 닫기)
            },
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 이미지를 가득 채워서 표시
                    permissionGuideImage?.invoke()
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 다시 안보기 버튼 (왼쪽)
                    TextButton(
                        onClick = {
                            preferencesManager.setPermissionGuideShown()
                            showPermissionGuideDialog = false
                        }
                    ) {
                        Text(strings.close)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 파싱 권한 버튼 (오른쪽)
                    TextButton(
                        onClick = {
                            notificationPermissionChecker.openListenerSettings()
                        }
                    ) {
                        Text(strings.parsingPermission)
                    }

                    // 푸시 권한 버튼 (오른쪽)
                    TextButton(
                        onClick = {
                            notificationPermissionChecker.openAppNotificationSettings()
                        }
                    ) {
                        Text(strings.pushPermission)
                    }
                }
            },
            dismissButton = null
        )
    }
}

@Composable
private fun PayPeriodHeader(
    selectedDate: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val year = selectedDate.year
    val month = selectedDate.monthNumber

    Text(
        text = strings.monthYear(year, month),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier.clickable(onClick = onClick)
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

    // 투자 관련 카테고리 목록
    val investmentCategories = setOf("투자", "손절", "익절", "배당금")

    // 투자 관련 항목 제외하고 계산
    val income = periodTransactions
        .filter { it.type == TransactionType.INCOME && it.category !in investmentCategories }
        .sumOf { it.displayAmount }
    val expense = periodTransactions
        .filter { it.type == TransactionType.EXPENSE && it.category !in investmentCategories }
        .sumOf { it.displayAmount }
    val balance = income - expense

    val strings = LocalStrings.current

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
                        text = strings.payPeriodSummary,
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
                            contentDescription = if (isMoneyVisible) strings.hideMoneyAmounts else strings.showMoneyAmounts,
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
                                text = strings.income,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "+${strings.amountWithUnit(Utils.formatAmount(income))}",
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
                                text = strings.expense,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "-${strings.amountWithUnit(Utils.formatAmount(expense))}",
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
    holidays: Set<LocalDate> = emptySet(),
    isMoveMode: Boolean = false,
    onDateSelected: (LocalDate) -> Unit,
    tutorialViewModel: com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel? = null
) {
    // 오늘 날짜 계산
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // 월급 기간에 포함되는 모든 날짜 계산
    val allDates = generateDateSequence(payPeriod.startDate, payPeriod.endDate)

    val strings = LocalStrings.current

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
            strings.weekdaysShort.forEach { day ->
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
            // 시작일의 요일을 직접 계산 (월=0, 일=6)
            val emptyDaysAtStart = payPeriod.startDate.dayOfWeek.ordinal
            items(emptyDaysAtStart) {
                Box(modifier = Modifier.height(40.dp))
            }

            // All dates in pay period
            items(allDates) { date ->
                val dayTransactions = transactions.filter { it.date == date }
                val hasIncome = dayTransactions.any { it.type == TransactionType.INCOME }
                val hasExpense = dayTransactions.any { it.type == TransactionType.EXPENSE }
                val hasSaving = dayTransactions.any { it.type == TransactionType.SAVING }
                val isInCurrentPeriod = date >= payPeriod.startDate && date <= payPeriod.endDate
                val dayOfWeek = date.dayOfWeek.ordinal // 0=Monday, 6=Sunday
                val isHoliday = holidays.contains(date)

                // 급여기간 시작일 또는 월이 바뀌는 1일에 "월/일" 형식 표시
                val displayText = if (date == payPeriod.startDate || date.dayOfMonth == 1) {
                    "${date.monthNumber}/${date.dayOfMonth}"
                } else {
                    date.dayOfMonth.toString()
                }

                CalendarDay(
                    displayText = displayText,
                    hasIncome = hasIncome,
                    hasExpense = hasExpense,
                    hasSaving = hasSaving,
                    isSelected = selectedDate == date,
                    isInCurrentPeriod = isInCurrentPeriod,
                    isToday = date == today,
                    dayOfWeek = dayOfWeek,
                    isHoliday = isHoliday,
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
    displayText: String,
    hasIncome: Boolean,
    hasExpense: Boolean,
    hasSaving: Boolean = false,
    isSelected: Boolean,
    isInCurrentPeriod: Boolean = true,
    isToday: Boolean = false,
    dayOfWeek: Int, // 0=Monday, 5=Saturday, 6=Sunday
    isHoliday: Boolean = false,
    onClick: () -> Unit
) {
    // 다크모드 확인: onSurface 색상이 밝으면 다크모드
    val isDarkMode = MaterialTheme.colorScheme.onSurface.red > 0.5f

    // 주말 및 공휴일 배경색 계산 (토요일: 파랑, 일요일/공휴일: 빨강)
    val weekendBackground = when {
        isHoliday || dayOfWeek == 6 -> if (isDarkMode) {
            Color(0xFFC62828).copy(alpha = 0.2f) // 일요일/공휴일 - 다크모드에서는 어두운 빨강 + 낮은 투명도
        } else {
            Color(0xFFFFEBEE).copy(alpha = 0.5f) // 일요일/공휴일 - 라이트모드: 연한 빨강
        }
        dayOfWeek == 5 -> if (isDarkMode) {
            Color(0xFF1565C0).copy(alpha = 0.2f) // 토요일 - 다크모드에서는 어두운 파랑 + 낮은 투명도
        } else {
            Color(0xFFE3F2FD).copy(alpha = 0.5f) // 토요일 - 라이트모드: 연한 파랑
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
            val isMonthLabel = displayText.contains("/")
            Text(
                text = displayText,
                fontSize = if (isMonthLabel) 11.sp else 14.sp,
                fontWeight = if (hasIncome || hasExpense || hasSaving) FontWeight.Bold else FontWeight.Normal,
                color = if (isInCurrentPeriod) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 인디케이터 점
            if (hasIncome || hasExpense || hasSaving) {
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
                    if (hasSaving) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(com.woojin.paymanagement.theme.SavingColor.color)
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
    holidayNames: Map<LocalDate, String> = emptyMap(),
    isMoveMode: Boolean = false,
    transactionToMove: Transaction? = null,
    availableCategories: List<com.woojin.paymanagement.data.Category> = emptyList(),
    onTransactionLongClick: (Transaction) -> Unit = {},
    onCancelMoveMode: () -> Unit = {},
    onClick: (LocalDate?) -> Unit = {},
    tutorialViewModel: com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel? = null
) {
    val strings = LocalStrings.current
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
                            text = "📍 ${strings.selectDateToMove}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = strings.cancel,
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
                                val holidayName = holidayNames[selectedDate]
                                val baseText = strings.dateTransactionHeader(selectedDate.monthNumber, selectedDate.dayOfMonth)
                                val holidayText = if (holidayName != null) " ($holidayName)" else ""
                                val countText = if (count > 0) " (${strings.transactionCount(count)})" else ""
                                "📝 $baseText$holidayText$countText"
                            } else {
                                "📝 ${strings.selectDateToViewMemo}"
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
                                onLongClick = { onTransactionLongClick(transaction) },
                                availableCategories = availableCategories
                            )
                        }
                    }
                } else if (selectedDate != null) {
                    Text(
                        text = strings.noTransactionsOnDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = strings.tapCalendarToViewMemo,
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
    onLongClick: () -> Unit = {},
    availableCategories: List<com.woojin.paymanagement.data.Category> = emptyList()
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
                    when (transaction.type) {
                        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                        TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                    }
                )
                .align(Alignment.Top)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val strings = LocalStrings.current
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
                        text = getCategoryEmoji(transaction.category, availableCategories),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 결제 수단 표시 (카테고리 옆에 괄호로)
                    val paymentMethodText = when (transaction.type) {
                        TransactionType.INCOME -> {
                            when (transaction.incomeType) {
                                com.woojin.paymanagement.data.IncomeType.CASH -> strings.cash
                                com.woojin.paymanagement.data.IncomeType.BALANCE_CARD -> "${strings.balanceCard} ${transaction.cardName ?: ""}"
                                com.woojin.paymanagement.data.IncomeType.GIFT_CARD -> "${strings.giftCard} ${transaction.cardName ?: ""}"
                                null -> strings.cash
                            }
                        }
                        TransactionType.EXPENSE -> {
                            when (transaction.paymentMethod) {
                                com.woojin.paymanagement.data.PaymentMethod.CASH -> strings.cash
                                com.woojin.paymanagement.data.PaymentMethod.CARD -> transaction.cardName ?: strings.card
                                com.woojin.paymanagement.data.PaymentMethod.BALANCE_CARD -> "${strings.balanceCard} ${transaction.cardName ?: ""}"
                                com.woojin.paymanagement.data.PaymentMethod.GIFT_CARD -> "${strings.giftCard} ${transaction.cardName ?: ""}"
                                null -> strings.cash
                            }
                        }
                        TransactionType.SAVING -> ""
                    }
                    if (paymentMethodText.isNotBlank()) {
                        Text(
                            text = "($paymentMethodText)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${when (transaction.type) {
                        TransactionType.INCOME -> "+"
                        TransactionType.EXPENSE -> "-"
                        TransactionType.SAVING -> "-"
                    }}${
                        strings.amountWithUnit(Utils.formatAmount(
                            transaction.displayAmount
                        ))
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (transaction.type) {
                        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                        TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = transaction.merchant ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 년/월 선택 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearMonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit
) {
    val strings = LocalStrings.current
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 제목
                Text(
                    text = strings.selectPayday,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 년도 선택
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.yearLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedYear -= 1 }) {
                            Icon(
                                Icons.Default.KeyboardArrowLeft,
                                contentDescription = strings.previousYear,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = strings.yearDisplay(selectedYear),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        IconButton(onClick = { selectedYear += 1 }) {
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = strings.nextYear,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 월 선택
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.monthLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            selectedMonth = if (selectedMonth == 1) 12 else selectedMonth - 1
                        }) {
                            Icon(
                                Icons.Default.KeyboardArrowLeft,
                                contentDescription = strings.previousMonthLabel,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = strings.monthDisplayShort(selectedMonth),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        IconButton(onClick = {
                            selectedMonth = if (selectedMonth == 12) 1 else selectedMonth + 1
                        }) {
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = strings.nextMonthLabel,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = {
                        onConfirm(selectedYear, selectedMonth)
                        onDismiss()
                    }) {
                        Text(strings.confirm)
                    }
                }
            }
        }
    }
}