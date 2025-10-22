package com.woojin.paymanagement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.database.DatabaseDriverFactory
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.di.databaseModule
import com.woojin.paymanagement.di.domainModule
import com.woojin.paymanagement.di.presentationModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.woojin.paymanagement.presentation.addtransaction.AddTransactionScreen
import com.woojin.paymanagement.presentation.calendar.CalendarScreen
import com.woojin.paymanagement.presentation.datedetail.DateDetailScreen
import com.woojin.paymanagement.presentation.paydaysetup.PaydaySetupScreen
import com.woojin.paymanagement.presentation.parsedtransaction.ParsedTransactionListScreen
import com.woojin.paymanagement.presentation.settings.ThemeSettingsDialog
import com.woojin.paymanagement.presentation.statistics.StatisticsScreen
import com.woojin.paymanagement.utils.PreferencesManager
import com.woojin.paymanagement.utils.ThemeMode
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module

// Koin 인스턴스를 저장할 변수
var koinInstance: Koin? = null

// Koin 의존성 주입을 위한 헬퍼 함수
inline fun <reified T> koinInject(): T = requireNotNull(koinInstance).get()

@Composable
fun App(
    databaseDriverFactory: DatabaseDriverFactory,
    preferencesManager: PreferencesManager,
    notificationPermissionChecker: com.woojin.paymanagement.utils.NotificationPermissionChecker,
    shouldNavigateToParsedTransactions: Boolean = false,
    onNavigationHandled: () -> Unit = {},
    onSendTestNotifications: ((List<com.woojin.paymanagement.data.ParsedTransaction>) -> Unit)? = null,
    onThemeChanged: (() -> Unit)? = null
) {
    var isKoinInitialized by remember { mutableStateOf(false) }

    // Koin 초기화
    LaunchedEffect(Unit) {
        initializeKoin(databaseDriverFactory, preferencesManager, notificationPermissionChecker)
        isKoinInitialized = true
    }

    MaterialTheme {
        if (isKoinInitialized) {
            PayManagementApp(
                shouldNavigateToParsedTransactions = shouldNavigateToParsedTransactions,
                onNavigationHandled = onNavigationHandled,
                onSendTestNotifications = onSendTestNotifications,
                onThemeChanged = onThemeChanged
            )
        } else {
            // 로딩 화면 또는 빈 화면
        }
    }
}

// Koin 초기화 함수
private fun initializeKoin(
    databaseDriverFactory: DatabaseDriverFactory,
    preferencesManager: PreferencesManager,
    notificationPermissionChecker: com.woojin.paymanagement.utils.NotificationPermissionChecker
) {
    try {
        val koin = startKoin {
            modules(
                // 플랫폼별 의존성들을 동적으로 제공하는 모듈
                module {
                    single<DatabaseDriverFactory> { databaseDriverFactory }
                    single<PreferencesManager> { preferencesManager }
                    single<com.woojin.paymanagement.utils.NotificationPermissionChecker> { notificationPermissionChecker }
                    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
                },
                // 공통 의존성들
                databaseModule,
                domainModule,
                presentationModule
            )
        }.koin

        // 전역 변수에 Koin 인스턴스 저장
        koinInstance = koin
    } catch (e: Exception) {
        // 이미 초기화된 경우 무시
    }
}

@Composable
fun PayManagementApp(
    shouldNavigateToParsedTransactions: Boolean = false,
    onNavigationHandled: () -> Unit = {},
    onSendTestNotifications: ((List<com.woojin.paymanagement.data.ParsedTransaction>) -> Unit)? = null,
    onThemeChanged: (() -> Unit)? = null
) {
    // DI로 의존성 주입받기
    val preferencesManager: PreferencesManager = koinInject()
    val databaseHelper: DatabaseHelper = koinInject()
    val scope = rememberCoroutineScope()

    // 초기 화면 결정 로직
    val initialScreen = when {
        !preferencesManager.isPaydaySet() -> Screen.PaydaySetup
        else -> Screen.Calendar
    }
    var currentScreen by remember { mutableStateOf(initialScreen) }
    var selectedDate by remember { mutableStateOf<LocalDate>(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    var editTransaction by remember { mutableStateOf<Transaction?>(null) }
    var previousScreen by remember { mutableStateOf(Screen.Calendar) }
    var selectedPayPeriod by remember { mutableStateOf<com.woojin.paymanagement.utils.PayPeriod?>(null) }
    var currentCalendarPayPeriod by remember { mutableStateOf<com.woojin.paymanagement.utils.PayPeriod?>(null) }
    var selectedParsedTransaction by remember { mutableStateOf<com.woojin.paymanagement.data.ParsedTransaction?>(null) }
    var showListenerPermissionDialog by remember { mutableStateOf(false) }
    var showPostPermissionDialog by remember { mutableStateOf(false) }

    // Deep link 처리: 푸시 알림에서 카드 결제 내역 화면으로 이동
    LaunchedEffect(shouldNavigateToParsedTransactions) {
        if (shouldNavigateToParsedTransactions) {
            // Payday가 설정되어 있을 때만 네비게이션 수행
            if (preferencesManager.isPaydaySet()) {
                currentScreen = Screen.ParsedTransactionList
            }
            onNavigationHandled()
        }
    }
    
    // 데이터베이스 초기화를 지연시켜 크래시 방지
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var availableBalanceCards by remember { mutableStateOf<List<com.woojin.paymanagement.data.BalanceCard>>(emptyList()) }
    var availableGiftCards by remember { mutableStateOf<List<com.woojin.paymanagement.data.GiftCard>>(emptyList()) }

    // 데이터베이스 데이터를 안전하게 로드
    LaunchedEffect(Unit) {
        try {
            databaseHelper.getAllTransactions().collect {
                transactions = it
            }
        } catch (e: Exception) {
            // 데이터베이스 오류 시 빈 리스트 유지
        }
    }

    LaunchedEffect(Unit) {
        try {
            databaseHelper.getActiveBalanceCards().collect {
                availableBalanceCards = it
            }
        } catch (e: Exception) { }
    }

    LaunchedEffect(Unit) {
        try {
            databaseHelper.getActiveGiftCards().collect {
                availableGiftCards = it
            }
        } catch (e: Exception) { }
    }

    // 알림 리스너 권한 다이얼로그
    if (showListenerPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showListenerPermissionDialog = false },
            title = { Text("알림 리스너 권한 필요") },
            text = { Text("카드 알림을 감지하려면 알림 리스너 권한이 필요합니다.\n\n설정 화면으로 이동하시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    showListenerPermissionDialog = false
                    val notificationPermissionChecker = koinInject<com.woojin.paymanagement.utils.NotificationPermissionChecker>()
                    notificationPermissionChecker.openListenerSettings()
                }) {
                    Text("설정하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showListenerPermissionDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 알림 전송 권한 다이얼로그
    if (showPostPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPostPermissionDialog = false },
            title = { Text("알림 전송 권한 필요") },
            text = { Text("앱 알림을 표시하려면 알림 전송 권한이 필요합니다.\n\n설정 화면으로 이동하시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    showPostPermissionDialog = false
                    val notificationPermissionChecker = koinInject<com.woojin.paymanagement.utils.NotificationPermissionChecker>()
                    notificationPermissionChecker.openAppNotificationSettings()
                }) {
                    Text("설정하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostPermissionDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 드로어 상태 및 테마 설정 다이얼로그 상태
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showThemeDialog by remember { mutableStateOf(false) }
    var currentThemeMode by remember { mutableStateOf(preferencesManager.getThemeMode()) }

    // 테마 설정 다이얼로그
    if (showThemeDialog) {
        ThemeSettingsDialog(
            currentThemeMode = currentThemeMode,
            onThemeModeSelected = { mode ->
                currentThemeMode = mode
                preferencesManager.setThemeMode(mode)
                onThemeChanged?.invoke() // 테마 변경 콜백 호출
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    BoxWithConstraints {
        val screenWidth = maxWidth

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen, // Drawer 열렸을 때만 제스처 활성화 (닫기 위해)
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(screenWidth * 0.6f) // 화면 너비의 60%
                        .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)), // 우측 상하단 둥글게
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                    ) {
                        // 상단: 제목 + X 버튼
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "설정",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "닫기",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider()

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // TODO: 여기에 다른 메뉴 아이템 추가 가능
                        }

                        // 하단: Color Scheme 설정 (고정)
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            HorizontalDivider()

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = "🎨",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "테마 설정",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Light/Dark 토글 스위치
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Light 버튼
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            color = if (currentThemeMode == ThemeMode.LIGHT)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            if (currentThemeMode != ThemeMode.LIGHT) {
                                                currentThemeMode = ThemeMode.LIGHT
                                                preferencesManager.setThemeMode(ThemeMode.LIGHT)
                                                onThemeChanged?.invoke()
                                            }
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "☀️",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Light",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (currentThemeMode == ThemeMode.LIGHT)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (currentThemeMode == ThemeMode.LIGHT)
                                            FontWeight.Bold
                                        else
                                            FontWeight.Normal
                                    )
                                }

                                // Dark 버튼
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            color = if (currentThemeMode == ThemeMode.DARK)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            if (currentThemeMode != ThemeMode.DARK) {
                                                currentThemeMode = ThemeMode.DARK
                                                preferencesManager.setThemeMode(ThemeMode.DARK)
                                                onThemeChanged?.invoke()
                                            }
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🌙",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Dark",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (currentThemeMode == ThemeMode.DARK)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (currentThemeMode == ThemeMode.DARK)
                                            FontWeight.Bold
                                        else
                                            FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
    ) {
        when (currentScreen) {
        Screen.PaydaySetup -> {
            // Koin에서 ViewModel 주입 (remember로 상태 유지)
            val paydaySetupViewModel = remember { koinInject<com.woojin.paymanagement.presentation.paydaysetup.PaydaySetupViewModel>() }

            PaydaySetupScreen(
                viewModel = paydaySetupViewModel,
                onSetupComplete = { payday, adjustment ->
                    preferencesManager.setPayday(payday)
                    preferencesManager.setPaydayAdjustment(adjustment)
                    currentScreen = Screen.Calendar
                }
            )
        }
        
        Screen.Calendar -> {
            // Koin에서 ViewModel 주입 (remember로 상태 유지)
            val calendarViewModel = remember { koinInject<com.woojin.paymanagement.presentation.calendar.CalendarViewModel>() }
            val tutorialViewModel = remember { koinInject<com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel>() }

            // ViewModel 초기화
            LaunchedEffect(Unit) {
                calendarViewModel.initializeCalendar(
                    transactions = transactions,
                    initialPayPeriod = currentCalendarPayPeriod,
                    selectedDate = selectedDate
                )
            }

            // 거래 내역 업데이트
            LaunchedEffect(transactions) {
                calendarViewModel.updateTransactions(transactions)
            }

            // ViewModel의 selectedDate와 App의 selectedDate 동기화
            LaunchedEffect(calendarViewModel.uiState.selectedDate) {
                calendarViewModel.uiState.selectedDate?.let { newSelectedDate ->
                    selectedDate = newSelectedDate
                }
            }

            CalendarScreen(
                viewModel = calendarViewModel,
                tutorialViewModel = tutorialViewModel,
                onOpenDrawer = {
                    scope.launch {
                        drawerState.open()
                    }
                },
                onDateDetailClick = { date ->
                    selectedDate = date
                    currentScreen = Screen.DateDetail
                },
                onStatisticsClick = { payPeriod ->
                    selectedPayPeriod = payPeriod
                    currentScreen = Screen.Statistics
                },
                onAddTransactionClick = {
                    editTransaction = null
                    previousScreen = Screen.Calendar
                    currentScreen = Screen.AddTransaction
                },
                onPayPeriodChanged = { payPeriod ->
                    currentCalendarPayPeriod = payPeriod
                },
                onParsedTransactionsClick = {
                    val notificationPermissionChecker = koinInject<com.woojin.paymanagement.utils.NotificationPermissionChecker>()

                    // 알림 리스너 권한만 체크 (카드 알림 감지용)
                    val hasListener = notificationPermissionChecker.hasListenerPermission()

                    if (hasListener) {
                        // 알림 리스너 권한이 있으면 화면 이동
                        currentScreen = Screen.ParsedTransactionList
                    } else {
                        // 알림 리스너 권한이 없으면 요청
                        showListenerPermissionDialog = true
                    }
                }
            )
        }
        
        Screen.Statistics -> {
            // Koin에서 ViewModel 주입 (remember로 상태 유지)
            val statisticsViewModel = remember { koinInject<com.woojin.paymanagement.presentation.statistics.StatisticsViewModel>() }

            StatisticsScreen(
                transactions = transactions,
                availableBalanceCards = availableBalanceCards,
                availableGiftCards = availableGiftCards,
                initialPayPeriod = selectedPayPeriod,
                onBack = {
                    currentScreen = Screen.Calendar
                },
                viewModel = statisticsViewModel
            )
        }
        
        Screen.AddTransaction -> {
            // Koin에서 ViewModel 주입 (remember로 상태 유지)
            val addTransactionViewModel = remember { koinInject<com.woojin.paymanagement.presentation.addtransaction.AddTransactionViewModel>() }

            AddTransactionScreen(
                transactions = transactions,
                selectedDate = selectedDate,
                editTransaction = editTransaction,
                parsedTransaction = selectedParsedTransaction,
                viewModel = addTransactionViewModel,
                onSave = { newTransactions ->
                    scope.launch {
                        if (editTransaction != null) {
                            // 편집 모드: 거래 업데이트는 이미 UseCase에서 처리됨
                        } else {
                            // 추가 모드: 새 거래들 추가 (복수 거래 가능)
                            newTransactions.forEach { transaction ->
                                // 잔액권 수입인 경우 잔액권 생성
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.INCOME &&
                                    transaction.incomeType == com.woojin.paymanagement.data.IncomeType.BALANCE_CARD &&
                                    transaction.balanceCardId != null && transaction.cardName != null) {
                                    val balanceCard = com.woojin.paymanagement.data.BalanceCard(
                                        id = transaction.balanceCardId,
                                        name = transaction.cardName,
                                        initialAmount = transaction.amount,
                                        currentBalance = transaction.amount,
                                        createdDate = transaction.date,
                                        isActive = true
                                    )
                                    databaseHelper.insertBalanceCard(balanceCard)
                                }

                                // 상품권 수입인 경우 상품권 생성
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.INCOME &&
                                    transaction.incomeType == com.woojin.paymanagement.data.IncomeType.GIFT_CARD &&
                                    transaction.giftCardId != null && transaction.cardName != null) {
                                    val giftCard = com.woojin.paymanagement.data.GiftCard(
                                        id = transaction.giftCardId,
                                        name = transaction.cardName,
                                        totalAmount = transaction.amount,
                                        usedAmount = 0.0,
                                        createdDate = transaction.date,
                                        isActive = true
                                    )
                                    databaseHelper.insertGiftCard(giftCard)
                                }

                                // 잔액권 지출인 경우 잔액권 잔액 업데이트
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.EXPENSE &&
                                    transaction.paymentMethod == com.woojin.paymanagement.data.PaymentMethod.BALANCE_CARD &&
                                    transaction.balanceCardId != null) {
                                    // 데이터베이스에서 최신 카드 정보 조회
                                    val currentCard = databaseHelper.getBalanceCardById(transaction.balanceCardId)
                                    if (currentCard != null) {
                                        val newBalance = currentCard.currentBalance - transaction.amount
                                        databaseHelper.updateBalanceCardBalance(
                                            id = currentCard.id,
                                            currentBalance = newBalance,
                                            isActive = newBalance > 0
                                        )
                                    }
                                }

                                // 상품권 지출인 경우 상품권 사용량 업데이트
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.EXPENSE &&
                                    transaction.paymentMethod == com.woojin.paymanagement.data.PaymentMethod.GIFT_CARD &&
                                    transaction.giftCardId != null) {
                                    // 데이터베이스에서 최신 카드 정보 조회
                                    val currentCard = databaseHelper.getGiftCardById(transaction.giftCardId)
                                    if (currentCard != null) {
                                        val newUsedAmount = currentCard.usedAmount + transaction.amount
                                        // 상품권이 한 번 사용되면 완전히 비활성화 (환급 발생)
                                        databaseHelper.updateGiftCardUsage(
                                            id = currentCard.id,
                                            usedAmount = newUsedAmount,
                                            isActive = false // 한 번 사용되면 완전히 비활성화
                                        )
                                    }
                                }

                                // 거래 저장은 이미 UseCase에서 처리됨
                            }

                            // 파싱된 거래에서 온 경우 처리됨 표시
                            selectedParsedTransaction?.let { parsedTransaction ->
                                val parsedTransactionViewModel = koinInject<com.woojin.paymanagement.presentation.parsedtransaction.ParsedTransactionViewModel>()
                                parsedTransactionViewModel.markAsProcessed(parsedTransaction.id)
                            }
                        }

                        // 파싱된 거래 상태 초기화
                        selectedParsedTransaction = null
                    }
                    currentScreen = previousScreen
                },
                onCancel = {
                    selectedParsedTransaction = null
                    currentScreen = previousScreen
                }
            )
        }
        
        Screen.DateDetail -> {
            // Koin에서 ViewModel 주입 (remember로 상태 유지)
            val dateDetailViewModel = remember { koinInject<com.woojin.paymanagement.presentation.datedetail.DateDetailViewModel>() }

            DateDetailScreen(
                selectedDate = selectedDate,
                transactions = transactions,
                viewModel = dateDetailViewModel,
                onBack = {
                    currentScreen = Screen.Calendar
                },
                onEditTransaction = { transaction ->
                    editTransaction = transaction
                    previousScreen = Screen.DateDetail
                    currentScreen = Screen.AddTransaction
                },
                onDeleteTransaction = { transaction ->
                    scope.launch {
                        databaseHelper.deleteTransaction(transaction.id)
                    }
                },
                onAddTransaction = {
                    editTransaction = null
                    previousScreen = Screen.DateDetail
                    currentScreen = Screen.AddTransaction
                }
            )
        }
        
        Screen.EditTransaction -> {
            // EditTransaction은 AddTransaction과 동일하게 처리
            val editTransactionViewModel = remember { koinInject<com.woojin.paymanagement.presentation.addtransaction.AddTransactionViewModel>() }

            AddTransactionScreen(
                transactions = transactions,
                selectedDate = selectedDate,
                editTransaction = editTransaction,
                viewModel = editTransactionViewModel,
                onSave = { newTransactions ->
                    scope.launch {
                        if (editTransaction != null) {
                            // 편집 모드: 거래 업데이트는 이미 UseCase에서 처리됨
                        } else {
                            newTransactions.forEach { transaction ->
                                // 잔액권 수입인 경우 잔액권 생성
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.INCOME &&
                                    transaction.incomeType == com.woojin.paymanagement.data.IncomeType.BALANCE_CARD &&
                                    transaction.balanceCardId != null && transaction.cardName != null) {
                                    val balanceCard = com.woojin.paymanagement.data.BalanceCard(
                                        id = transaction.balanceCardId,
                                        name = transaction.cardName,
                                        initialAmount = transaction.amount,
                                        currentBalance = transaction.amount,
                                        createdDate = transaction.date,
                                        isActive = true
                                    )
                                    databaseHelper.insertBalanceCard(balanceCard)
                                }

                                // 상품권 수입인 경우 상품권 생성
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.INCOME &&
                                    transaction.incomeType == com.woojin.paymanagement.data.IncomeType.GIFT_CARD &&
                                    transaction.giftCardId != null && transaction.cardName != null) {
                                    val giftCard = com.woojin.paymanagement.data.GiftCard(
                                        id = transaction.giftCardId,
                                        name = transaction.cardName,
                                        totalAmount = transaction.amount,
                                        usedAmount = 0.0,
                                        createdDate = transaction.date,
                                        isActive = true
                                    )
                                    databaseHelper.insertGiftCard(giftCard)
                                }

                                // 잔액권 지출인 경우 잔액권 잔액 업데이트
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.EXPENSE &&
                                    transaction.paymentMethod == com.woojin.paymanagement.data.PaymentMethod.BALANCE_CARD &&
                                    transaction.balanceCardId != null) {
                                    // 데이터베이스에서 최신 카드 정보 조회
                                    val currentCard = databaseHelper.getBalanceCardById(transaction.balanceCardId)
                                    if (currentCard != null) {
                                        val newBalance = currentCard.currentBalance - transaction.amount
                                        databaseHelper.updateBalanceCardBalance(
                                            id = currentCard.id,
                                            currentBalance = newBalance,
                                            isActive = newBalance > 0
                                        )
                                    }
                                }

                                // 상품권 지출인 경우 상품권 사용량 업데이트
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.EXPENSE &&
                                    transaction.paymentMethod == com.woojin.paymanagement.data.PaymentMethod.GIFT_CARD &&
                                    transaction.giftCardId != null) {
                                    // 데이터베이스에서 최신 카드 정보 조회
                                    val currentCard = databaseHelper.getGiftCardById(transaction.giftCardId)
                                    if (currentCard != null) {
                                        val newUsedAmount = currentCard.usedAmount + transaction.amount
                                        // 상품권이 한 번 사용되면 완전히 비활성화 (환급 발생)
                                        databaseHelper.updateGiftCardUsage(
                                            id = currentCard.id,
                                            usedAmount = newUsedAmount,
                                            isActive = false // 한 번 사용되면 완전히 비활성화
                                        )
                                    }
                                }

                                // 거래 저장은 이미 UseCase에서 처리됨
                            }
                        }
                    }
                    currentScreen = Screen.DateDetail
                },
                onCancel = {
                    currentScreen = Screen.DateDetail
                }
            )
        }

        Screen.ParsedTransactionList -> {
            val parsedTransactionViewModel = remember { koinInject<com.woojin.paymanagement.presentation.parsedtransaction.ParsedTransactionViewModel>() }
            val notificationPermissionChecker = koinInject<com.woojin.paymanagement.utils.NotificationPermissionChecker>()

            ParsedTransactionListScreen(
                viewModel = parsedTransactionViewModel,
                onTransactionClick = { parsedTransaction ->
                    selectedParsedTransaction = parsedTransaction
                    previousScreen = Screen.ParsedTransactionList
                    currentScreen = Screen.AddTransaction
                },
                onBack = {
                    currentScreen = Screen.Calendar
                },
                onSendTestNotifications = onSendTestNotifications,
                hasNotificationPermission = notificationPermissionChecker.hasPostNotificationPermission(),
                onRequestNotificationPermission = {
                    notificationPermissionChecker.openAppNotificationSettings()
                },
                onCheckPermission = {
                    notificationPermissionChecker.hasPostNotificationPermission()
                }
            )
        }
    }
    } // BoxWithConstraints 닫기
    }
}

enum class Screen {
    PaydaySetup,
    Calendar,
    Statistics,
    AddTransaction,
    DateDetail,
    EditTransaction,
    ParsedTransactionList
}