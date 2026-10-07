package com.woojin.paymanagement.presentation.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.presentation.addtransaction.formatCategoryDisplay
import com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji
import com.woojin.paymanagement.presentation.tutorial.CalendarTutorialOverlay
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.theme.BrandColor
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.Utils
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    tutorialViewModel: com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel,
    preferencesManager: com.woojin.paymanagement.utils.PreferencesManager,
    notificationPermissionChecker: com.woojin.paymanagement.utils.NotificationPermissionChecker? = null,
    onOpenDrawer: () -> Unit = {},
    onDateDetailClick: (LocalDate) -> Unit = {},
    onStatisticsClick: (PayPeriod) -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    onPayPeriodChanged: (PayPeriod) -> Unit = {},
    onParsedTransactionsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAppExit: () -> Unit = {},
    nativeAdContent: @Composable (() -> Unit)? = null,
    hasNativeAd: Boolean = false,
    calendarNativeAdContent: @Composable (() -> Unit)? = null,
    exitDialogBannerContent: @Composable (() -> Unit)? = null,
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

    // 날짜가 바뀌었는지 확인 (앱을 켜 둔 채 자정이 지나거나, 다음 날 다시 열었을 때)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onDayChanged()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            viewModel.onDayChanged()
        }
    }

    // 뒤로가기 핸들링 - 앱 종료 확인
    com.woojin.paymanagement.utils.BackHandler {
        showExitDialog = true
    }

    // EdgeToEdge 대응은 CalendarTutorialOverlay에서 처리됩니다

    var fabExpanded by remember { mutableStateOf(false) }
    // 길게 눌러 날짜 이동 안내: 한 번 사용하기 전까지만 표시
    var isMoveHintSeen by remember { mutableStateOf(preferencesManager.isMoveTransactionHintSeen()) }

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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
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
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 공유 모드 토글 (공유방에 있을 때만 표시)
                        if (uiState.isInSharedRoom) {
                            SharedModeToggle(
                                isSharedMode = uiState.isSharedMode,
                                onClick = { viewModel.toggleSharedMode() }
                            )
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = strings.search,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                // Pay Period가 null인 경우에도 메뉴 버튼 표시
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = strings.openMenu,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = strings.search,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (uiState.currentPayPeriod != null) {

                Spacer(modifier = Modifier.height(16.dp))

                // Pay Period Summary
                PayPeriodSummaryCard(
                    transactions = if (uiState.isSharedMode)
                        uiState.sharedTransactions.map { it.transaction }
                    else
                        uiState.transactions,
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
                    today = uiState.today,
                    transactions = if (uiState.isSharedMode)
                        uiState.sharedTransactions.map { it.transaction }
                    else
                        uiState.transactions,
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

                Spacer(modifier = Modifier.height(12.dp))

                // 달력과 상세 내역 사이 구분 띠 (화면 좌우 끝까지)
                SectionBand()

                Spacer(modifier = Modifier.height(8.dp))

                // Daily Transaction Display
                DailyTransactionCard(
                    selectedDate = uiState.selectedDate,
                    transactions = if (uiState.isSharedMode)
                        uiState.sharedTransactions.map { it.transaction }
                    else
                        uiState.transactions,
                    myTransactionIds = if (uiState.isSharedMode)
                        uiState.sharedTransactions.filter { it.isMine }.map { it.transaction.id }.toSet()
                    else
                        null,
                    isSharedMode = uiState.isSharedMode,
                    holidayNames = uiState.holidayNames,
                    isMoveMode = uiState.isMoveMode,
                    transactionToMove = uiState.transactionToMove,
                    availableCategories = uiState.availableCategories,
                    onTransactionLongClick = { transaction ->
                        viewModel.startMoveMode(transaction)
                        if (!isMoveHintSeen) {
                            preferencesManager.setMoveTransactionHintSeen()
                            isMoveHintSeen = true
                        }
                    },
                    showMoveHint = !isMoveHintSeen,
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

                // 캘린더 화면 네이티브 광고
                if (calendarNativeAdContent != null && !preferencesManager.isAdRemovalActive()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    calendarNativeAdContent()
                }
            } else {
                // 로딩 상태 표시 - Shimmer Effect
                CalendarScreenShimmer()
            }

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

        // 공유 모드 연결 오류 배너
        uiState.sharedError?.let { errorMsg ->
            LaunchedEffect(errorMsg) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearSharedError()
            }
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 80.dp + navigationBarHeight)
            ) {
                Text(errorMsg)
            }
        }
    }

    // 앱 종료 확인 다이얼로그
    if (showExitDialog) {
        var showButtons by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if ((exitDialogBannerContent != null || hasNativeAd) && !preferencesManager.isAdRemovalActive()) {
                delay(2000)
            }
            showButtons = true
        }

        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(strings.exitConfirmMessage) },
            text = {
                if (!preferencesManager.isAdRemovalActive()) {
                    if (exitDialogBannerContent != null) {
                        exitDialogBannerContent()
                    } else if (hasNativeAd) {
                        nativeAdContent?.invoke()
                    }
                }
            },
            confirmButton = {
                AnimatedVisibility(
                    visible = showButtons,
                    enter = fadeIn(animationSpec = tween(300))
                ) {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onAppExit()
                        }
                    ) {
                        Text(strings.exitConfirm)
                    }
                }
            },
            dismissButton = {
                AnimatedVisibility(
                    visible = showButtons,
                    enter = fadeIn(animationSpec = tween(300))
                ) {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(strings.cancel)
                    }
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

    val income = periodTransactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.displayAmount }
    val expense = periodTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.displayAmount }
    val saving = periodTransactions
        .filter { it.type == TransactionType.SAVING }
        .sumOf { it.displayAmount }
    val investmentIncomeCategories = setOf("익절", "배당금")
    val investment = periodTransactions
        .filter { it.type == TransactionType.INVESTMENT }
        .sumOf { t -> if (t.category in investmentIncomeCategories) t.displayAmount else -t.displayAmount }
    val balance = income - expense

    val strings = LocalStrings.current
    val onCard = Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                tutorialViewModel?.updateTargetBounds(
                    "pay_period_summary",
                    bounds
                )
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(BrandColor.mint, Color(0xFF47B49C))
                )
            )
            .clickable { onStatisticsClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column {
            // 헤더: 제목 + 급여 기간 + 금액 숨김(자물쇠)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 ${strings.payPeriodSummary}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = onCard
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(onCard.copy(alpha = 0.2f))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = strings.payPeriodRange(
                            payPeriod.startDate.monthNumber,
                            payPeriod.startDate.dayOfMonth,
                            payPeriod.endDate.monthNumber,
                            payPeriod.endDate.dayOfMonth
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onCard,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                androidx.compose.material3.IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = if (isMoneyVisible) strings.hideMoneyAmounts else strings.showMoneyAmounts,
                        tint = if (isMoneyVisible) onCard.copy(alpha = 0.5f) else onCard,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1줄: 수입 / 지출 / 잔액
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryValue(
                    label = strings.income, emoji = null,
                    value = "+${strings.amountWithUnit(Utils.formatAmount(income))}",
                    align = Alignment.Start, isMoneyVisible = isMoneyVisible, modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = strings.expense, emoji = null,
                    value = "-${strings.amountWithUnit(Utils.formatAmount(expense))}",
                    align = Alignment.CenterHorizontally, isMoneyVisible = isMoneyVisible, modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = strings.balance, emoji = null,
                    value = "${
                        when {
                            balance > 0 -> "+"
                            balance < 0 -> "-"
                            else -> ""
                        }
                    }${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(balance)))}",
                    align = Alignment.End, isMoneyVisible = isMoneyVisible, modifier = Modifier.weight(1f)
                )
            }

            // 2줄: 저축 / 투자 (해당 거래가 있을 때만 표시 - 기존 조건 유지)
            val hasSaving = saving > 0
            val hasInvestment = investment != 0.0
            if (hasSaving || hasInvestment) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(onCard.copy(alpha = 0.25f))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (hasSaving) {
                        SummaryValue(
                            label = strings.saving, emoji = "🐷",
                            value = "-${strings.amountWithUnit(Utils.formatAmount(saving))}",
                            align = Alignment.Start, isMoneyVisible = isMoneyVisible, modifier = Modifier.weight(1f)
                        )
                    }
                    if (hasInvestment) {
                        SummaryValue(
                            label = strings.investment, emoji = "💹",
                            value = "${if (investment > 0) "+" else "-"}${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(investment)))}",
                            // 저축이 있으면 위 '지출' 칸과 같은 가운데 열, 없으면 첫 칸
                            align = if (hasSaving) Alignment.CenterHorizontally else Alignment.Start,
                            isMoneyVisible = isMoneyVisible,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 남는 칸을 비워서 위 3칸과 열을 맞춤
                    repeat(3 - (if (hasSaving) 1 else 0) - (if (hasInvestment) 1 else 0)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** 급여 기간 요약 카드의 라벨 + 금액 한 칸 (금액 숨김 시 블러 - 기존 동작 유지) */
@Composable
private fun SummaryValue(
    label: String,
    emoji: String?,
    value: String,
    align: Alignment.Horizontal,
    isMoneyVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(
            text = if (emoji != null) "$emoji $label" else label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            modifier = if (!isMoneyVisible) Modifier.blur(8.dp) else Modifier
        )
    }
}

/**
 * 화면 좌우 끝까지 이어지는 회색 구분 띠.
 * 부모 Column의 좌우 패딩(16dp)을 무시하고 전체 폭으로 그림.
 */
@Composable
private fun SectionBand(horizontalBleed: androidx.compose.ui.unit.Dp = 16.dp) {
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
private fun CalendarGrid(
    payPeriod: PayPeriod,
    today: LocalDate,
    transactions: List<Transaction>,
    selectedDate: LocalDate?,
    holidays: Set<LocalDate> = emptySet(),
    isMoveMode: Boolean = false,
    onDateSelected: (LocalDate) -> Unit,
    tutorialViewModel: com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel? = null
) {

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
        val emptyDaysAtStart = (payPeriod.startDate.dayOfWeek.ordinal + 1) % 7
        val rowCount = (emptyDaysAtStart + allDates.size + 6) / 7
        val gridHeight = (rowCount * 40 + (rowCount - 1) * 4).dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(gridHeight),
            userScrollEnabled = false
        ) {
            // Empty cells for days before first date starts
            // kotlinx.datetime ordinal은 ISO 8601 기준 (월=0, ..., 일=6)
            // 헤더가 일~토(일=0, 월=1, ..., 토=6)이므로 +1 후 %7로 변환
            items(emptyDaysAtStart) {
                Box(modifier = Modifier.height(40.dp))
            }

            // All dates in pay period
            items(allDates) { date ->
                val dayTransactions = transactions.filter { it.date == date }
                val hasIncome = dayTransactions.any { it.type == TransactionType.INCOME }
                val hasExpense = dayTransactions.any { it.type == TransactionType.EXPENSE }
                val hasSaving = dayTransactions.any { it.type == TransactionType.SAVING }
                val hasInvestment = dayTransactions.any { it.type == TransactionType.INVESTMENT }
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
                    hasInvestment = hasInvestment,
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
    hasInvestment: Boolean = false,
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
                fontWeight = if (hasIncome || hasExpense || hasSaving || hasInvestment) FontWeight.Bold else FontWeight.Normal,
                color = if (isInCurrentPeriod) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 인디케이터 점
            if (hasIncome || hasExpense || hasSaving || hasInvestment) {
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
                    if (hasInvestment) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(com.woojin.paymanagement.theme.InvestmentColor.color)
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
    myTransactionIds: Set<String>? = null,
    isSharedMode: Boolean = false,
    holidayNames: Map<LocalDate, String> = emptyMap(),
    isMoveMode: Boolean = false,
    transactionToMove: Transaction? = null,
    availableCategories: List<com.woojin.paymanagement.data.Category> = emptyList(),
    onTransactionLongClick: (Transaction) -> Unit = {},
    showMoveHint: Boolean = false,
    onCancelMoveMode: () -> Unit = {},
    onClick: (LocalDate?) -> Unit = {},
    tutorialViewModel: com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel? = null
) {
    val strings = LocalStrings.current
    val dayTransactions = selectedDate?.let { date ->
        transactions.filter { it.date == date }
    } ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                tutorialViewModel?.updateTargetBounds(
                    "transaction_card",
                    bounds
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(selectedDate) }
            .padding(vertical = 4.dp)
    ) {
        // 헤더 또는 이동 모드 메시지
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMoveMode) {
                Text(
                    text = "📍 ${strings.selectDateToMove}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandColor.mint,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = strings.cancel,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onCancelMoveMode() }
                        .padding(4.dp)
                )
            } else if (selectedDate != null) {
                val weekday = when (selectedDate.dayOfWeek) {
                    kotlinx.datetime.DayOfWeek.MONDAY -> strings.monday
                    kotlinx.datetime.DayOfWeek.TUESDAY -> strings.tuesday
                    kotlinx.datetime.DayOfWeek.WEDNESDAY -> strings.wednesday
                    kotlinx.datetime.DayOfWeek.THURSDAY -> strings.thursday
                    kotlinx.datetime.DayOfWeek.FRIDAY -> strings.friday
                    kotlinx.datetime.DayOfWeek.SATURDAY -> strings.saturday
                    else -> strings.sunday
                }
                val holidayName = holidayNames[selectedDate]
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.shortDate(selectedDate.monthNumber, selectedDate.dayOfMonth)} (${strings.weekdayShort(weekday)})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (holidayName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = holidayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (dayTransactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.transactionCount(dayTransactions.size),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // 날짜 상세 화면으로 이동할 수 있다는 표시 (영역 전체 클릭 동작은 기존과 동일)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.viewDetails,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandColor.mint
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BrandColor.mint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Text(
                    text = strings.selectDateToViewMemo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (dayTransactions.isNotEmpty()) {
            // 스크롤 가능한 거래 목록 (높이 제한 + 내부 스크롤은 기존 방식 유지)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(dayTransactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        isSelected = isMoveMode && transaction.id == transactionToMove?.id,
                        onLongClick = { onTransactionLongClick(transaction) },
                        availableCategories = availableCategories,
                        showMineIndicator = isSharedMode && myTransactionIds != null && transaction.id in myTransactionIds
                    )
                }
            }
            // 길게 눌러 날짜 이동 안내 (처음 한 번 사용하기 전까지만)
            if (showMoveHint && !isMoveMode) {
                Text(
                    text = "💡 ${strings.moveTransactionHint}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp)
                )
            }
        } else {
            Text(
                text = if (selectedDate != null) strings.noTransactionsOnDate else strings.tapCalendarToViewMemo,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    transaction: Transaction,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    availableCategories: List<com.woojin.paymanagement.data.Category> = emptyList(),
    showMineIndicator: Boolean = false
) {
    val strings = LocalStrings.current
    val typeColor = when (transaction.type) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
        TransactionType.INVESTMENT -> com.woojin.paymanagement.theme.InvestmentColor.color
    }

    // 결제 수단 텍스트 (기존 규칙 그대로)
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
        TransactionType.INVESTMENT -> ""
    }.trim()
    // 두 번째 줄: 사용처 · 결제수단
    val subText = listOf(transaction.merchant.orEmpty().trim(), paymentMethodText)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .background(
                if (isSelected) BrandColor.mint.copy(alpha = 0.12f) else Color.Transparent
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 카테고리 아이콘 타일 (거래 유형 색으로 은은하게)
        val categoryEmoji = getCategoryEmoji(transaction.category, availableCategories)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(typeColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (categoryEmoji.isNotBlank()) {
                Text(text = categoryEmoji, fontSize = 19.sp)
            } else {
                Text(
                    text = transaction.category.take(1),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatCategoryDisplay(transaction.category, transaction.subCategory),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (showMineIndicator) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "나",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            if (subText.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val investmentIncomeCategories = setOf("익절", "배당금")
        Text(
            text = "${when (transaction.type) {
                TransactionType.INCOME -> "+"
                TransactionType.EXPENSE -> "-"
                TransactionType.SAVING -> "-"
                TransactionType.INVESTMENT -> if (transaction.category in investmentIncomeCategories) "+" else "-"
            }}${
                strings.amountWithUnit(Utils.formatAmount(
                    transaction.displayAmount
                ))
            }",
            fontSize = 14.5.sp,
            color = typeColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SharedModeToggle(
    isSharedMode: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSharedMode)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (isSharedMode)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isSharedMode) "공유" else "개인",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}