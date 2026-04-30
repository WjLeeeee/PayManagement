package com.woojin.paymanagement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.woojin.paymanagement.presentation.addtransaction.AddTransactionScreen
import com.woojin.paymanagement.presentation.calculator.CalculatorDialog
import com.woojin.paymanagement.presentation.calendar.CalendarScreen
import com.woojin.paymanagement.presentation.datedetail.DateDetailScreen
import com.woojin.paymanagement.presentation.monthlycomparison.MonthlyComparisonScreen
import com.woojin.paymanagement.presentation.parsedtransaction.ParsedTransactionListScreen
import com.woojin.paymanagement.presentation.paydaysetup.PaydaySetupScreen
import com.woojin.paymanagement.presentation.settings.ThemeSettingsDialog
import com.woojin.paymanagement.presentation.search.SearchScreen
import com.woojin.paymanagement.presentation.statistics.StatisticsScreen
import com.woojin.paymanagement.utils.LifecycleObserverHelper
import com.woojin.paymanagement.utils.PreferencesManager
import com.woojin.paymanagement.utils.ThemeMode
import com.woojin.paymanagement.strings.Language
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.strings.ProvideStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module

// Koin 인스턴스를 저장할 변수
var koinInstance: Koin? = null

// Koin 의존성 주입을 위한 헬퍼 함수
inline fun <reified T> koinInject(): T = requireNotNull(koinInstance).get()

@Composable
fun App(
    modifier: Modifier = Modifier,
    databaseDriverFactory: DatabaseDriverFactory,
    preferencesManager: PreferencesManager,
    notificationPermissionChecker: com.woojin.paymanagement.utils.NotificationPermissionChecker,
    appInfo: com.woojin.paymanagement.utils.AppInfo,
    fileHandler: com.woojin.paymanagement.utils.FileHandler,
    billingClient: com.woojin.paymanagement.utils.BillingClient,
    autoExecuteNotifier: com.woojin.paymanagement.utils.AutoExecuteNotifier = com.woojin.paymanagement.utils.NoOpAutoExecuteNotifier(),
    interstitialAdManager: com.woojin.paymanagement.utils.InterstitialAdManager? = null,
    shouldNavigateToParsedTransactions: Boolean = false,
    shouldNavigateToRecurringTransactions: Boolean = false,
    shouldNavigateToAdRemoval: Boolean = false,
    onParsedTransactionsNavigationHandled: () -> Unit = {},
    onRecurringTransactionsNavigationHandled: () -> Unit = {},
    onAdRemovalNavigationHandled: () -> Unit = {},
    onThemeChanged: (() -> Unit)? = null,
    onRequestPostNotificationPermission: ((onPermissionResult: (Boolean) -> Unit) -> Unit)? = null,
    onLaunchSaveFile: (String) -> Unit = {},
    onLaunchLoadFile: () -> Unit = {},
    onAppExit: () -> Unit = {},
    onContactSupport: () -> Unit = {},
    nativeAdContent: @Composable () -> Unit = {},
    hasNativeAd: Boolean = false,
    exitDialogBannerContent: @Composable (() -> Unit)? = null,
    comparisonNativeAdContent: @Composable (() -> Unit)? = null,
    permissionGuideImage: @Composable (() -> Unit)? = null,
    onRequestReview: () -> Unit = {}
) {
    var isKoinInitialized by remember { mutableStateOf(false) }

    // Koin 초기화
    LaunchedEffect(Unit) {
        initializeKoin(databaseDriverFactory, preferencesManager, notificationPermissionChecker, appInfo, fileHandler, billingClient, autoExecuteNotifier)
        isKoinInitialized = true
    }

    // 앱 리뷰 요청 조건 체크 (거래가 추가될 때마다 재확인)
    LaunchedEffect(isKoinInitialized) {
        if (!isKoinInitialized) return@LaunchedEffect
        if (preferencesManager.isReviewRequested()) return@LaunchedEffect

        val databaseHelper: DatabaseHelper = koinInject()
        databaseHelper.getAllTransactions().collect { transactions ->
            if (transactions.size >= 10 && !preferencesManager.isReviewRequested()) {
                preferencesManager.setReviewRequested()
                onRequestReview()
            }
        }
    }

    var languageCode by remember { mutableStateOf(preferencesManager.getLanguageCode()) }

    MaterialTheme {
        if (isKoinInitialized) {
            ProvideStrings(languageCode) {
                PayManagementApp(
                modifier = modifier,
                interstitialAdManager = interstitialAdManager,
                shouldNavigateToParsedTransactions = shouldNavigateToParsedTransactions,
                shouldNavigateToRecurringTransactions = shouldNavigateToRecurringTransactions,
                shouldNavigateToAdRemoval = shouldNavigateToAdRemoval,
                onParsedTransactionsNavigationHandled = onParsedTransactionsNavigationHandled,
                onRecurringTransactionsNavigationHandled = onRecurringTransactionsNavigationHandled,
                onAdRemovalNavigationHandled = onAdRemovalNavigationHandled,
                onThemeChanged = onThemeChanged,
                onRequestPostNotificationPermission = onRequestPostNotificationPermission,
                onLaunchSaveFile = onLaunchSaveFile,
                onLaunchLoadFile = onLaunchLoadFile,
                fileHandler = fileHandler,
                onAppExit = onAppExit,
                onContactSupport = onContactSupport,
                onLanguageChanged = { newCode ->
                    preferencesManager.setLanguageCode(newCode)
                    languageCode = newCode
                },
                nativeAdContent = nativeAdContent,
                hasNativeAd = hasNativeAd,
                exitDialogBannerContent = exitDialogBannerContent,
                comparisonNativeAdContent = comparisonNativeAdContent,
                permissionGuideImage = permissionGuideImage
            )
            }
        } else {
            // 로딩 화면 또는 빈 화면
        }
    }
}

// Koin 초기화 함수
private fun initializeKoin(
    databaseDriverFactory: DatabaseDriverFactory,
    preferencesManager: PreferencesManager,
    notificationPermissionChecker: com.woojin.paymanagement.utils.NotificationPermissionChecker,
    appInfo: com.woojin.paymanagement.utils.AppInfo,
    fileHandler: com.woojin.paymanagement.utils.FileHandler,
    billingClient: com.woojin.paymanagement.utils.BillingClient,
    autoExecuteNotifier: com.woojin.paymanagement.utils.AutoExecuteNotifier = com.woojin.paymanagement.utils.NoOpAutoExecuteNotifier()
) {
    try {
        val koin = startKoin {
            modules(
                // 플랫폼별 의존성들을 동적으로 제공하는 모듈
                module {
                    single<DatabaseDriverFactory> { databaseDriverFactory }
                    single<PreferencesManager> { preferencesManager }
                    single<com.woojin.paymanagement.utils.NotificationPermissionChecker> { notificationPermissionChecker }
                    single<com.woojin.paymanagement.utils.AppInfo> { appInfo }
                    single<com.woojin.paymanagement.utils.FileHandler> { fileHandler }
                    single<com.woojin.paymanagement.utils.BillingClient> { billingClient }
                    single<com.woojin.paymanagement.utils.AutoExecuteNotifier> { autoExecuteNotifier }
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
    modifier: Modifier = Modifier,
    interstitialAdManager: com.woojin.paymanagement.utils.InterstitialAdManager? = null,
    shouldNavigateToParsedTransactions: Boolean = false,
    shouldNavigateToRecurringTransactions: Boolean = false,
    shouldNavigateToAdRemoval: Boolean = false,
    onParsedTransactionsNavigationHandled: () -> Unit = {},
    onRecurringTransactionsNavigationHandled: () -> Unit = {},
    onAdRemovalNavigationHandled: () -> Unit = {},
    onThemeChanged: (() -> Unit)? = null,
    onRequestPostNotificationPermission: ((onPermissionResult: (Boolean) -> Unit) -> Unit)? = null,
    onLaunchSaveFile: (String) -> Unit = {},
    onLaunchLoadFile: () -> Unit = {},
    fileHandler: com.woojin.paymanagement.utils.FileHandler? = null,
    onAppExit: () -> Unit = {},
    onContactSupport: () -> Unit = {},
    onLanguageChanged: ((String) -> Unit)? = null,
    nativeAdContent: @Composable () -> Unit = {},
    hasNativeAd: Boolean = false,
    exitDialogBannerContent: @Composable (() -> Unit)? = null,
    comparisonNativeAdContent: @Composable (() -> Unit)? = null,
    permissionGuideImage: @Composable (() -> Unit)? = null
) {
    // DI로 의존성 주입받기
    val preferencesManager: PreferencesManager = koinInject()
    val databaseHelper: DatabaseHelper = koinInject()
    val categoryRepository: com.woojin.paymanagement.domain.repository.CategoryRepository = koinInject()
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    // 초기 카테고리 설정
    LaunchedEffect(Unit) {
        categoryRepository.initializeDefaultCategories()
    }

    // 초기 화면 결정 로직
    val initialScreen = when {
        !preferencesManager.isPaydaySet() -> Screen.PaydaySetup
        else -> Screen.Calendar
    }

    // 네비게이션 스택 관리
    var navigationStack by remember { mutableStateOf(listOf(initialScreen)) }
    val currentScreen = navigationStack.last()

    // 화면 변경 시 Analytics 로깅
    LaunchedEffect(currentScreen) {
        val analyticsLogger = com.woojin.paymanagement.analytics.Analytics.getInstance()
        val screenName = when (currentScreen) {
            Screen.PaydaySetup -> "월급날_설정"
            Screen.Calendar -> "홈_캘린더"
            Screen.Statistics -> "분석_통계"
            Screen.AddTransaction -> "거래_추가"
            Screen.DateDetail -> "날짜_상세"
            Screen.EditTransaction -> "거래_수정"
            Screen.ParsedTransactionList -> "파싱_거래_목록"
            Screen.CategoryManagement -> "카테고리_관리"
            Screen.CardManagement -> "결제수단_관리"
            Screen.BudgetSettings -> "예산_설정"
            Screen.MonthlyComparison -> "월별_비교"
            Screen.TipDonation -> "팁_후원"
            Screen.AdRemoval -> "광고_제거"
            Screen.Coupon -> "쿠폰_입력"
            Screen.RecurringTransaction -> "반복_거래"
            Screen.TransactionSearch -> "거래_검색"
        }

        analyticsLogger.logScreenView(
            screenName = screenName,
            screenClass = currentScreen.name
        )
    }

    // 네비게이션 헬퍼 함수들
    fun navigateTo(screen: Screen) {
        navigationStack = navigationStack + screen
    }

    fun navigateBack() {
        if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
        }
    }

    fun navigateToRoot(screen: Screen) {
        navigationStack = listOf(screen)
    }

    var selectedDate by remember { mutableStateOf<LocalDate>(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    var editTransaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedPayPeriod by remember { mutableStateOf<com.woojin.paymanagement.utils.PayPeriod?>(null) }
    var currentCalendarPayPeriod by remember { mutableStateOf<com.woojin.paymanagement.utils.PayPeriod?>(null) }
    var selectedParsedTransaction by remember { mutableStateOf<com.woojin.paymanagement.data.ParsedTransaction?>(null) }
    var selectedRecurringTransaction by remember { mutableStateOf<com.woojin.paymanagement.data.RecurringTransaction?>(null) }
    var showListenerPermissionDialog by remember { mutableStateOf(false) }
    var showPostPermissionDialog by remember { mutableStateOf(false) }
    var budgetExceededMessage by remember { mutableStateOf<String?>(null) }
    var payPeriodChangedMessage by remember { mutableStateOf<String?>(null) }
    var shouldShowPreviousPeriodComparison by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 예산 초과 스낵바 표시
    LaunchedEffect(budgetExceededMessage) {
        budgetExceededMessage?.let { message ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = strings.confirm,
                    withDismissAction = false,
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    // 확인 버튼 클릭 시 예산 설정 화면으로 이동
                    navigateTo(Screen.BudgetSettings)
                }
                // 스낵바가 사라지면 메시지 초기화
                budgetExceededMessage = null
            }
        }
    }

    // 급여 기간 변경 스낵바 표시
    LaunchedEffect(payPeriodChangedMessage) {
        payPeriodChangedMessage?.let { message ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = strings.confirm,
                    withDismissAction = false,
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    // 확인 버튼 클릭 시 급여 기간 비교 화면으로 이동 (직직전 vs 직전)
                    shouldShowPreviousPeriodComparison = true
                    navigateTo(Screen.MonthlyComparison)
                }
                // 스낵바가 사라지면 메시지 초기화
                payPeriodChangedMessage = null
            }
        }
    }

    // 앱 시작 시 급여 기간 변경 체크
    LaunchedEffect(Unit) {
        if (preferencesManager.isPaydaySet()) {
            val payPeriodCalculator = koinInject<com.woojin.paymanagement.utils.PayPeriodCalculator>()
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val payday = preferencesManager.getPayday()
            val adjustment = preferencesManager.getPaydayAdjustment()
            val currentPayPeriod = payPeriodCalculator.getCurrentPayPeriod(payday, adjustment, today)

            val lastChecked = preferencesManager.getLastCheckedPayPeriodStartDate()
            val currentStartDate = "${currentPayPeriod.startDate}"

            if (lastChecked != null && lastChecked != currentStartDate) {
                // 급여 기간이 변경됨!
                payPeriodChangedMessage = strings.newPayPeriodStarted + " 📊"
            }

            // 현재 기간 저장
            preferencesManager.setLastCheckedPayPeriodStartDate(currentStartDate)
        }
    }

    // Deep link 처리: 푸시 알림에서 카드 결제 내역 화면으로 이동
    LaunchedEffect(shouldNavigateToParsedTransactions) {
        if (shouldNavigateToParsedTransactions) {
            // Payday가 설정되어 있을 때만 네비게이션 수행
            if (preferencesManager.isPaydaySet()) {
                navigateTo(Screen.ParsedTransactionList)
            }
            onParsedTransactionsNavigationHandled()
        }
    }

    // Deep link 처리: 푸시 알림에서 반복 거래 관리 화면으로 이동
    LaunchedEffect(shouldNavigateToRecurringTransactions) {
        if (shouldNavigateToRecurringTransactions) {
            // Payday가 설정되어 있을 때만 네비게이션 수행
            if (preferencesManager.isPaydaySet()) {
                navigateTo(Screen.RecurringTransaction)
            }
            onRecurringTransactionsNavigationHandled()
        }
    }

    // 광고 제거 화면으로 이동
    LaunchedEffect(shouldNavigateToAdRemoval) {
        if (shouldNavigateToAdRemoval) {
            // Payday가 설정되어 있을 때만 네비게이션 수행
            if (preferencesManager.isPaydaySet()) {
                navigateTo(Screen.AdRemoval)
            }
            onAdRemovalNavigationHandled()
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
            title = { Text(strings.notificationListenerPermission) },
            text = { Text(strings.notificationListenerPermissionDesc) },
            confirmButton = {
                Button(onClick = {
                    showListenerPermissionDialog = false
                    val notificationPermissionChecker = koinInject<com.woojin.paymanagement.utils.NotificationPermissionChecker>()
                    notificationPermissionChecker.openListenerSettings()
                }) {
                    Text(strings.goToSettings)
                }
            },
            dismissButton = {
                TextButton(onClick = { showListenerPermissionDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // 알림 전송 권한 다이얼로그
    if (showPostPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPostPermissionDialog = false },
            title = { Text(strings.postNotificationPermission) },
            text = { Text(strings.postNotificationPermissionDesc) },
            confirmButton = {
                Button(onClick = {
                    showPostPermissionDialog = false
                    val notificationPermissionChecker = koinInject<com.woojin.paymanagement.utils.NotificationPermissionChecker>()
                    notificationPermissionChecker.openAppNotificationSettings()
                }) {
                    Text(strings.goToSettings)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostPermissionDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // 드로어 상태 및 테마 설정 다이얼로그 상태
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showThemeDialog by remember { mutableStateOf(false) }
    var currentThemeMode by remember { mutableStateOf(preferencesManager.getThemeMode()) }
    var showPaydayChangeDialog by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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

    // 언어 선택 다이얼로그
    if (showLanguageDialog) {
        val currentLang = preferencesManager.getLanguageCode()
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.selectLanguage) },
            text = {
                Column {
                    listOf(
                        "ko" to strings.korean,
                        "en" to strings.english
                    ).forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageChanged?.invoke(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLang == code,
                                onClick = {
                                    onLanguageChanged?.invoke(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // 월급날 변경 다이얼로그
    if (showPaydayChangeDialog) {
        AlertDialog(
            onDismissRequest = { showPaydayChangeDialog = false },
            title = { Text(strings.paydayChange) },
            text = {
                Text(strings.paydayChangeConfirm + "\n" + strings.paydayChangeDesc)
            },
            confirmButton = {
                Button(onClick = {
                    showPaydayChangeDialog = false
                    navigateTo(Screen.PaydaySetup)
                }) {
                    Text(strings.goToPaydaySettings)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaydayChangeDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val screenWidth = maxWidth

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen, // Drawer 열렸을 때만 제스처 활성화 (닫기 위해)
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(screenWidth * 0.7f) // 화면 너비의 70%
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
                                .padding(start = 16.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.settings,
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
                                    contentDescription = strings.close,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp)
                        ) {
                            // 월급날 변경 버튼
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.paydayChange,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = strings.paydayDisplay(preferencesManager.getPayday()),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    showPaydayChangeDialog = true
                                    scope.launch {
                                        drawerState.close()
                                    }
                                },
                                icon = {
                                    Text(
                                        text = "📅",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                modifier = Modifier.height(38.dp)
                            )

                            // 확장 가능한 메뉴 관리
                            var expandedMenu by remember { mutableStateOf<ExpandableMenu?>(null) }

                            // 푸시 알림 설정 섹션 (확장 가능)
                            val notificationPermissionChecker = koinInject<com.woojin.paymanagement.utils.NotificationPermissionChecker>()
                            var hasListenerPermission by remember { mutableStateOf(notificationPermissionChecker.hasListenerPermission()) }
                            var hasPostPermission by remember { mutableStateOf(notificationPermissionChecker.hasPostNotificationPermission()) }

                            // Drawer가 열릴 때마다 권한 상태 갱신
                            LaunchedEffect(drawerState.isOpen) {
                                if (drawerState.isOpen) {
                                    hasListenerPermission = notificationPermissionChecker.hasListenerPermission()
                                    hasPostPermission = notificationPermissionChecker.hasPostNotificationPermission()
                                }
                            }

                            // 앱이 다시 포커스를 받았을 때 권한 상태 갱신 (설정에서 돌아올 때)
                            val lifecycleObserver = remember { LifecycleObserverHelper() }
                            lifecycleObserver.ObserveLifecycle {
                                if (drawerState.isOpen) {
                                    hasListenerPermission = notificationPermissionChecker.hasListenerPermission()
                                    hasPostPermission = notificationPermissionChecker.hasPostNotificationPermission()
                                }
                            }

                            // 푸시 알림 메인 항목
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.pushNotifications,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = if (expandedMenu == ExpandableMenu.NOTIFICATION)
                                                Icons.Default.KeyboardArrowUp
                                            else
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (expandedMenu == ExpandableMenu.NOTIFICATION) strings.fold else strings.expand,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    expandedMenu = if (expandedMenu == ExpandableMenu.NOTIFICATION) null else ExpandableMenu.NOTIFICATION
                                },
                                icon = {
                                    Text(
                                        text = "🔔",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                modifier = Modifier.height(38.dp)
                            )

                            // 확장된 알림 설정 항목들 (애니메이션 적용)
                            AnimatedVisibility(
                                visible = expandedMenu == ExpandableMenu.NOTIFICATION,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, top = 4.dp, bottom = 8.dp)
                                ) {
                                    // 카드 알림 감지
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                notificationPermissionChecker.openListenerSettings()
                                            },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = strings.cardNotificationDetection,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = strings.cardNotificationDetectionDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = hasListenerPermission,
                                            onCheckedChange = {
                                                notificationPermissionChecker.openListenerSettings()
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 앱 알림
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (hasPostPermission) {
                                                    // ON → OFF: 설정 화면으로
                                                    notificationPermissionChecker.openAppNotificationSettings()
                                                } else {
                                                    // OFF → ON: 권한 요청
                                                    onRequestPostNotificationPermission?.invoke { isGranted ->
                                                        hasPostPermission = isGranted
                                                        // 권한이 거부되었으면 설정 화면으로 안내
                                                        if (!isGranted) {
                                                            notificationPermissionChecker.openAppNotificationSettings()
                                                        }
                                                    }
                                                }
                                            },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = strings.appNotification,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = strings.appNotificationDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = hasPostPermission,
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) {
                                                    // OFF → ON: 권한 요청
                                                    onRequestPostNotificationPermission?.invoke { isGranted ->
                                                        hasPostPermission = isGranted
                                                        // 권한이 거부되었으면 설정 화면으로 안내
                                                        if (!isGranted) {
                                                            notificationPermissionChecker.openAppNotificationSettings()
                                                        }
                                                    }
                                                } else {
                                                    // ON → OFF: 설정 화면으로
                                                    notificationPermissionChecker.openAppNotificationSettings()
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.dataManagement,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = if (expandedMenu == ExpandableMenu.DATA_MANAGEMENT)
                                                Icons.Default.KeyboardArrowUp
                                            else
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (expandedMenu == ExpandableMenu.DATA_MANAGEMENT) strings.fold else strings.expand,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    expandedMenu = if (expandedMenu == ExpandableMenu.DATA_MANAGEMENT) null else ExpandableMenu.DATA_MANAGEMENT
                                },
                                icon = {
                                    Text(
                                        text = "💾",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                modifier = Modifier.height(38.dp)
                            )

                            // 확장된 데이터 관리 항목들 (애니메이션 적용)
                            AnimatedVisibility(
                                visible = expandedMenu == ExpandableMenu.DATA_MANAGEMENT,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, top = 4.dp, bottom = 8.dp)
                                ) {
                                    // 데이터 내보내기
                                    val exportDataUseCase = koinInject<com.woojin.paymanagement.domain.usecase.ExportDataUseCase>()
                                    // fileHandler는 파라미터로 전달받음 (MainActivity와 동일한 인스턴스 사용)
                                    var showExportMessage by remember { mutableStateOf<String?>(null) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                scope.launch {
                                                    val result = exportDataUseCase(com.woojin.paymanagement.domain.model.BackupType.ALL)
                                                    result.onSuccess { jsonString ->
                                                        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                                                        val fileName = "backup_${currentDate}.json"
                                                        fileHandler?.setSaveData(
                                                            fileName = fileName,
                                                            jsonContent = jsonString,
                                                            onSuccess = {
                                                                showExportMessage = strings.exportSuccess
                                                                scope.launch { drawerState.close() }
                                                            },
                                                            onError = { error ->
                                                                showExportMessage = "${strings.exportFailed}: $error"
                                                            }
                                                        )
                                                        onLaunchSaveFile(fileName)
                                                    }.onFailure { error ->
                                                        showExportMessage = "${strings.exportFailed}: ${error.message}"
                                                    }
                                                }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📤",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = strings.exportData,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = strings.exportDataDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 데이터 가져오기
                                    val importDataUseCase = koinInject<com.woojin.paymanagement.domain.usecase.ImportDataUseCase>()
                                    var showImportMessage by remember { mutableStateOf<String?>(null) }
                                    var showReplaceConfirmDialog by remember { mutableStateOf(false) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                showReplaceConfirmDialog = true
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📥",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = strings.importData,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = strings.importDataDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // 가져오기 확인 다이얼로그
                                    if (showReplaceConfirmDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showReplaceConfirmDialog = false },
                                            title = { Text(strings.importConfirm) },
                                            text = { Text(strings.importConfirmDesc) },
                                            confirmButton = {
                                                Button(onClick = {
                                                    showReplaceConfirmDialog = false
                                                    fileHandler?.setLoadCallbacks(
                                                        onSuccess = { jsonString ->
                                                            scope.launch {
                                                                val result = importDataUseCase(jsonString, replaceExisting = true)
                                                                result.onSuccess { importResult ->
                                                                    showImportMessage = strings.importResult(importResult.successCount, importResult.failureCount)
                                                                    scope.launch { drawerState.close() }
                                                                }.onFailure { error ->
                                                                    showImportMessage = "${strings.importFailed}: ${error.message}"
                                                                }
                                                            }
                                                        },
                                                        onError = { error ->
                                                            showImportMessage = "${strings.fileLoadFailed}: $error"
                                                        }
                                                    )
                                                    onLaunchLoadFile()
                                                }) {
                                                    Text(strings.importData)
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showReplaceConfirmDialog = false }) {
                                                    Text(strings.cancel)
                                                }
                                            }
                                        )
                                    }

                                    // 내보내기/가져오기 메시지 표시
                                    showExportMessage?.let { message ->
                                        AlertDialog(
                                            onDismissRequest = { showExportMessage = null },
                                            title = { Text(strings.notice) },
                                            text = { Text(message) },
                                            confirmButton = {
                                                Button(onClick = { showExportMessage = null }) {
                                                    Text(strings.confirm)
                                                }
                                            }
                                        )
                                    }

                                    showImportMessage?.let { message ->
                                        AlertDialog(
                                            onDismissRequest = { showImportMessage = null },
                                            title = { Text(strings.notice) },
                                            text = { Text(message) },
                                            confirmButton = {
                                                Button(onClick = { showImportMessage = null }) {
                                                    Text(strings.confirm)
                                                }
                                            }
                                        )
                                    }

                                }
                            }

                            // 거래 도구 섹션
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.transactionTools,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = if (expandedMenu == ExpandableMenu.TRANSACTION_TOOLS)
                                                Icons.Default.KeyboardArrowUp
                                            else
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (expandedMenu == ExpandableMenu.TRANSACTION_TOOLS) strings.fold else strings.expand,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    expandedMenu = if (expandedMenu == ExpandableMenu.TRANSACTION_TOOLS) null else ExpandableMenu.TRANSACTION_TOOLS
                                },
                                icon = {
                                    Text(
                                        text = "🛠️",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                modifier = Modifier.height(38.dp)
                            )

                            // 확장된 거래 도구 항목들
                            AnimatedVisibility(
                                visible = expandedMenu == ExpandableMenu.TRANSACTION_TOOLS,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, top = 4.dp, bottom = 8.dp)
                                ) {
                                    // 반복 거래 관리
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                navigateTo(Screen.RecurringTransaction)
                                                scope.launch { drawerState.close() }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🔄",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = strings.recurringTransactions,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = strings.autoAddDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 결제수단 관리 (잔액권/상품권 + 카드 관리 통합)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                navigateTo(Screen.CardManagement)
                                                scope.launch { drawerState.close() }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "💳",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = strings.paymentMethodManagement,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = strings.paymentMethodManagementDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 카테고리 관리
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                navigateTo(Screen.CategoryManagement)
                                                scope.launch { drawerState.close() }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📂",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = strings.categoryManagement,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = strings.addIncomeExpenseCategory("${strings.income}/${strings.expense}/${strings.saving}"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                }
                            }

                            // 분석 & 예산 섹션
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${strings.statistics} & ${strings.budgetSettings}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = if (expandedMenu == ExpandableMenu.ANALYSIS)
                                                Icons.Default.KeyboardArrowUp
                                            else
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (expandedMenu == ExpandableMenu.ANALYSIS) strings.fold else strings.expand,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    expandedMenu = if (expandedMenu == ExpandableMenu.ANALYSIS) null else ExpandableMenu.ANALYSIS
                                },
                                icon = {
                                    Text(
                                        text = "📊",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                modifier = Modifier.height(38.dp)
                            )

                            // 확장된 분석 & 예산 항목들
                            AnimatedVisibility(
                                visible = expandedMenu == ExpandableMenu.ANALYSIS,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, top = 4.dp, bottom = 8.dp)
                                ) {
                                    // 계산기
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                showCalculatorDialog = true
                                                scope.launch { drawerState.close() }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🔢",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = strings.calculator,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = strings.spendingTrend,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 예산 설정
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                navigateTo(Screen.BudgetSettings)
                                                scope.launch { drawerState.close() }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "💵",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = strings.budgetSettings,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = strings.categoryBudget,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 급여 기간 비교
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                navigateTo(Screen.MonthlyComparison)
                                                scope.launch { drawerState.close() }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📈",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = strings.payPeriodComparison,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = strings.compareWithPreviousPeriod,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // 인앱 구매 (Android만)
                            if (com.woojin.paymanagement.utils.Platform.isAndroid()) {
                                // 메인 아이템: 인앱 구매
                                NavigationDrawerItem(
                                    label = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = strings.supportDeveloper,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                imageVector = if (expandedMenu == ExpandableMenu.IN_APP_PURCHASE)
                                                    Icons.Default.KeyboardArrowUp
                                                else
                                                    Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (expandedMenu == ExpandableMenu.IN_APP_PURCHASE) strings.fold else strings.expand,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    selected = false,
                                    onClick = {
                                        expandedMenu = if (expandedMenu == ExpandableMenu.IN_APP_PURCHASE) null else ExpandableMenu.IN_APP_PURCHASE
                                    },
                                    icon = {
                                        Text(
                                            text = "💰",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    modifier = Modifier.height(38.dp)
                                )

                                // 확장된 인앱 구매 항목들
                                AnimatedVisibility(
                                    visible = expandedMenu == ExpandableMenu.IN_APP_PURCHASE,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, top = 4.dp, bottom = 8.dp)
                                    ) {
                                        // 개발자 응원하기
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    navigateTo(Screen.TipDonation)
                                                    scope.launch { drawerState.close() }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "☕",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = strings.tipDonation,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "${strings.smallTip}, ${strings.mediumTip}, ${strings.largeTip}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // 광고 제거
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    navigateTo(Screen.AdRemoval)
                                                    scope.launch { drawerState.close() }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🚫",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = strings.adRemoval,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = strings.adRemovalDesc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // 쿠폰 입력
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    navigateTo(Screen.Coupon)
                                                    scope.launch { drawerState.close() }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🎫",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = strings.enterCoupon,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = strings.couponCode,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 언어 설정
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = strings.languageSettings,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                selected = false,
                                onClick = {
                                    showLanguageDialog = true
                                    scope.launch { drawerState.close() }
                                },
                                icon = {
                                    Text(
                                        text = "🌐",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                modifier = Modifier.height(48.dp)
                            )

                            // 관리자에게 문의
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = strings.contactSupport,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                selected = false,
                                onClick = {
                                    onContactSupport()
                                    scope.launch { drawerState.close() }
                                },
                                icon = {
                                    Text(
                                        text = "📧",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                modifier = Modifier.height(48.dp)
                            )

                            val appInfo = koinInject<com.woojin.paymanagement.utils.AppInfo>()
                            NavigationDrawerItem(
                                label = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = strings.aboutApp,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${strings.version} ${appInfo.getVersionName()}, Code ${appInfo.getVersionCode()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                selected = false,
                                onClick = { /* 앱 정보 표시 */ },
                                icon = {
                                    Text(
                                        text = "ℹ️",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                modifier = Modifier.height(48.dp)
                            )
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
                                    text = strings.themeSettings,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Light/Dark 토글 스위치
                            val isSystemInDarkTheme = isSystemInDarkTheme()
                            val isLightSelected = when (currentThemeMode) {
                                ThemeMode.LIGHT -> true
                                ThemeMode.DARK -> false
                                ThemeMode.SYSTEM -> !isSystemInDarkTheme
                            }
                            val isDarkSelected = when (currentThemeMode) {
                                ThemeMode.DARK -> true
                                ThemeMode.LIGHT -> false
                                ThemeMode.SYSTEM -> isSystemInDarkTheme
                            }

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
                                            color = if (isLightSelected)
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
                                        color = if (isLightSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isLightSelected)
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
                                            color = if (isDarkSelected)
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
                                        color = if (isDarkSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isDarkSelected)
                                            FontWeight.Bold
                                        else
                                            FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            when (currentScreen) {
            Screen.PaydaySetup -> {
            // Koin에서 ViewModel 주입 (remember로 상태 유지)
            val paydaySetupViewModel = remember { koinInject<com.woojin.paymanagement.presentation.paydaysetup.PaydaySetupViewModel>() }

            PaydaySetupScreen(
                viewModel = paydaySetupViewModel,
                onSetupComplete = { payday, adjustment ->
                    preferencesManager.setPayday(payday)
                    preferencesManager.setPaydayAdjustment(adjustment)

                    // 월급날 변경 후 액티비티 재시작 (초기 설정인지 변경인지 확인)
                    if (currentScreen == Screen.PaydaySetup && initialScreen != Screen.PaydaySetup) {
                        // 월급날 변경인 경우 액티비티 재시작
                        onThemeChanged?.invoke()
                    } else {
                        // 초기 설정인 경우 캘린더로 이동
                        navigateToRoot(Screen.Calendar)
                    }
                }
            )
        }
        
        Screen.Calendar -> {
            // Koin에서 ViewModel 주입 (remember로 상태 유지)
            val calendarViewModel = remember { koinInject<com.woojin.paymanagement.presentation.calendar.CalendarViewModel>() }
            val tutorialViewModel = remember { koinInject<com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel>() }
            val notificationPermissionChecker = koinInject<com.woojin.paymanagement.utils.NotificationPermissionChecker>()

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
                preferencesManager = preferencesManager,
                notificationPermissionChecker = notificationPermissionChecker,
                nativeAdContent = nativeAdContent,
                hasNativeAd = hasNativeAd,
                exitDialogBannerContent = exitDialogBannerContent,
                onRequestPostNotificationPermission = onRequestPostNotificationPermission,
                permissionGuideImage = permissionGuideImage,
                onOpenDrawer = {
                    scope.launch {
                        drawerState.open()
                    }
                },
                onDateDetailClick = { date ->
                    selectedDate = date
                    navigateTo(Screen.DateDetail)
                },
                onStatisticsClick = { payPeriod ->
                    selectedPayPeriod = payPeriod
                    navigateTo(Screen.Statistics)
                },
                onAddTransactionClick = {
                    editTransaction = null
                    navigateTo(Screen.AddTransaction)
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
                        navigateTo(Screen.ParsedTransactionList)
                    } else {
                        // 알림 리스너 권한이 없으면 요청
                        showListenerPermissionDialog = true
                    }
                },
                onSearchClick = { navigateTo(Screen.TransactionSearch) },
                onAppExit = onAppExit
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
                onBack = { navigateBack() },
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
                recurringTransaction = selectedRecurringTransaction,
                viewModel = addTransactionViewModel,
                onSave = { newTransactions, budgetMessage ->
                    scope.launch {
                        if (editTransaction != null) {
                            // 편집 모드: 거래 업데이트는 이미 UseCase에서 처리됨
                        } else {
                            // 추가 모드: 새 거래들 추가 (복수 거래 가능)
                            newTransactions.forEach { transaction ->
                                // 잔액권 수입인 경우 잔액권 생성 또는 충전
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.INCOME &&
                                    transaction.incomeType == com.woojin.paymanagement.data.IncomeType.BALANCE_CARD &&
                                    transaction.balanceCardId != null && transaction.cardName != null) {

                                    // 기존 잔액권 확인
                                    val existingCard = databaseHelper.getBalanceCardById(transaction.balanceCardId)

                                    if (existingCard != null) {
                                        // 기존 잔액권 충전 - 금액 추가
                                        val updatedCard = existingCard.copy(
                                            currentBalance = existingCard.currentBalance + transaction.amount
                                        )
                                        databaseHelper.updateBalanceCard(updatedCard)
                                    } else {
                                        // 새 잔액권 추가
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
                                        databaseHelper.updateGiftCardUsage(
                                            id = currentCard.id,
                                            usedAmount = newUsedAmount,
                                            isActive = newUsedAmount < currentCard.totalAmount
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

                            // 반복 거래에서 온 경우 lastExecutedDate 업데이트
                            selectedRecurringTransaction?.let { recurringTransaction ->
                                val markRecurringTransactionExecutedUseCase = koinInject<com.woojin.paymanagement.domain.usecase.MarkRecurringTransactionExecutedUseCase>()
                                markRecurringTransactionExecutedUseCase(recurringTransaction.id)
                            }
                        }

                        // 상태 초기화
                        selectedParsedTransaction = null
                        selectedRecurringTransaction = null

                        // 예산 초과 메시지 설정
                        budgetExceededMessage = budgetMessage
                    }
                    navigateBack()
                },
                onCancel = {
                    selectedParsedTransaction = null
                    selectedRecurringTransaction = null
                    navigateBack()
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
                onBack = { navigateBack() },
                onEditTransaction = { transaction ->
                    editTransaction = transaction
                    navigateTo(Screen.AddTransaction)
                },
                onDeleteTransaction = { transaction ->
                    scope.launch {
                        databaseHelper.deleteTransaction(transaction.id)
                    }
                },
                onAddTransaction = {
                    editTransaction = null
                    navigateTo(Screen.AddTransaction)
                },
                nativeAdContent = nativeAdContent,
                hasNativeAd = hasNativeAd
            )
        }
        
        Screen.EditTransaction -> {
            // EditTransaction은 AddTransaction과 동일하게 처리
            val editTransactionViewModel = remember { koinInject<com.woojin.paymanagement.presentation.addtransaction.AddTransactionViewModel>() }

            AddTransactionScreen(
                transactions = transactions,
                selectedDate = selectedDate,
                editTransaction = editTransaction,
                recurringTransaction = null,
                viewModel = editTransactionViewModel,
                onSave = { newTransactions, budgetMessage ->
                    scope.launch {
                        if (editTransaction != null) {
                            // 편집 모드: 거래 업데이트는 이미 UseCase에서 처리됨
                        } else {
                            newTransactions.forEach { transaction ->
                                // 잔액권 수입인 경우 잔액권 생성 또는 충전
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.INCOME &&
                                    transaction.incomeType == com.woojin.paymanagement.data.IncomeType.BALANCE_CARD &&
                                    transaction.balanceCardId != null && transaction.cardName != null) {

                                    // 기존 잔액권 확인
                                    val existingCard = databaseHelper.getBalanceCardById(transaction.balanceCardId)

                                    if (existingCard != null) {
                                        // 기존 잔액권 충전 - 금액 추가
                                        val updatedCard = existingCard.copy(
                                            currentBalance = existingCard.currentBalance + transaction.amount
                                        )
                                        databaseHelper.updateBalanceCard(updatedCard)
                                    } else {
                                        // 새 잔액권 추가
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
                                        databaseHelper.updateGiftCardUsage(
                                            id = currentCard.id,
                                            usedAmount = newUsedAmount,
                                            isActive = newUsedAmount < currentCard.totalAmount
                                        )
                                    }
                                }

                                // 거래 저장은 이미 UseCase에서 처리됨
                            }
                        }

                        // 예산 초과 메시지 설정
                        budgetExceededMessage = budgetMessage
                    }
                    navigateBack()
                },
                onCancel = { navigateBack() }
            )
        }

        Screen.ParsedTransactionList -> {
            val parsedTransactionViewModel = remember { koinInject<com.woojin.paymanagement.presentation.parsedtransaction.ParsedTransactionViewModel>() }
            val notificationPermissionChecker = koinInject<com.woojin.paymanagement.utils.NotificationPermissionChecker>()

            ParsedTransactionListScreen(
                viewModel = parsedTransactionViewModel,
                onTransactionClick = { parsedTransaction ->
                    selectedParsedTransaction = parsedTransaction
                    navigateTo(Screen.AddTransaction)
                },
                onBack = { navigateBack() },
                hasNotificationPermission = notificationPermissionChecker.hasPostNotificationPermission(),
                onRequestPostNotificationPermission = onRequestPostNotificationPermission,
                onOpenNotificationSettings = {
                    notificationPermissionChecker.openAppNotificationSettings()
                },
                onCheckPermission = {
                    notificationPermissionChecker.hasPostNotificationPermission()
                }
            )
        }

        Screen.CategoryManagement -> {
            val categoryManagementViewModel = remember { koinInject<com.woojin.paymanagement.presentation.categorymanagement.CategoryManagementViewModel>() }

            com.woojin.paymanagement.presentation.categorymanagement.CategoryManagementScreen(
                viewModel = categoryManagementViewModel,
                onNavigateBack = { navigateBack() }
            )
        }

        Screen.CardManagement -> {
            val cardManagementViewModel = remember { koinInject<com.woojin.paymanagement.presentation.cardmanagement.CardManagementViewModel>() }

            com.woojin.paymanagement.presentation.cardmanagement.CardManagementScreen(
                viewModel = cardManagementViewModel,
                onNavigateBack = { navigateBack() }
            )
        }

        Screen.BudgetSettings -> {
            val budgetSettingsViewModel = remember { koinInject<com.woojin.paymanagement.presentation.budgetsettings.BudgetSettingsViewModel>() }

            com.woojin.paymanagement.presentation.budgetsettings.BudgetSettingsScreen(
                viewModel = budgetSettingsViewModel,
                onNavigateBack = { navigateBack() },
                onNavigateToCategoryManagement = {
                    navigateTo(Screen.CategoryManagement)
                }
            )
        }

        Screen.MonthlyComparison -> {
            val monthlyComparisonViewModel = remember { koinInject<com.woojin.paymanagement.presentation.monthlycomparison.MonthlyComparisonViewModel>() }

            MonthlyComparisonScreen(
                viewModel = monthlyComparisonViewModel,
                onBack = {
                    shouldShowPreviousPeriodComparison = false
                    navigateBack()
                },
                showPreviousPeriodComparison = shouldShowPreviousPeriodComparison,
                nativeAdContent = comparisonNativeAdContent,
                hasNativeAd = comparisonNativeAdContent != null
            )
        }

        Screen.TipDonation -> {
            val tipDonationViewModel = remember { koinInject<com.woojin.paymanagement.presentation.tipdonation.TipDonationViewModel>() }

            com.woojin.paymanagement.presentation.tipdonation.TipDonationScreen(
                viewModel = tipDonationViewModel,
                onNavigateBack = { navigateBack() }
            )
        }

        Screen.AdRemoval -> {
            val adRemovalViewModel = remember { koinInject<com.woojin.paymanagement.presentation.adremoval.AdRemovalViewModel>() }

            com.woojin.paymanagement.presentation.adremoval.AdRemovalScreen(
                viewModel = adRemovalViewModel,
                onNavigateBack = { navigateBack() },
                onRequestRestart = onThemeChanged
            )
        }

        Screen.Coupon -> {
            val couponViewModel = remember { koinInject<com.woojin.paymanagement.presentation.coupon.CouponViewModel>() }

            com.woojin.paymanagement.presentation.coupon.CouponScreen(
                viewModel = couponViewModel,
                onNavigateBack = { navigateBack() },
                onRequestRestart = onThemeChanged
            )
        }

        Screen.RecurringTransaction -> {
            val recurringTransactionViewModel = remember { koinInject<com.woojin.paymanagement.presentation.recurringtransaction.RecurringTransactionViewModel>() }

            com.woojin.paymanagement.presentation.recurringtransaction.RecurringTransactionScreen(
                viewModel = recurringTransactionViewModel,
                onNavigateBack = { navigateBack() },
                onNavigateToAddTransaction = { recurringTransaction ->
                    selectedRecurringTransaction = recurringTransaction
                    editTransaction = null
                    selectedParsedTransaction = null
                    navigateTo(Screen.AddTransaction)
                }
            )
        }

        Screen.TransactionSearch -> {
            val searchViewModel = remember { koinInject<com.woojin.paymanagement.presentation.search.SearchViewModel>() }

            SearchScreen(
                viewModel = searchViewModel,
                onNavigateBack = { navigateBack() }
            )
        }
        }
    } // Scaffold 닫기

    // 계산기 다이얼로그
    if (showCalculatorDialog) {
        val categoryRepository = remember { koinInject<com.woojin.paymanagement.domain.repository.CategoryRepository>() }
        val categories by categoryRepository.getAllCategories().collectAsState(initial = emptyList())
        val payPeriodCalculator = remember { koinInject<com.woojin.paymanagement.utils.PayPeriodCalculator>() }

        var currentPayPeriod by remember { mutableStateOf<com.woojin.paymanagement.utils.PayPeriod?>(null) }

        LaunchedEffect(Unit) {
            val payday = preferencesManager.getPayday()
            val adjustment = preferencesManager.getPaydayAdjustment()
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            currentPayPeriod = payPeriodCalculator.getCurrentPayPeriod(
                currentDate = today,
                payday = payday,
                adjustment = adjustment
            )
        }

        currentPayPeriod?.let { period ->
            CalculatorDialog(
                transactions = transactions,
                onDismiss = { showCalculatorDialog = false },
                initialPayPeriod = period,
                allCategories = categories
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
    ParsedTransactionList,
    CategoryManagement,
    CardManagement,
    BudgetSettings,
    MonthlyComparison,
    TipDonation,
    AdRemoval,
    Coupon,
    RecurringTransaction,
    TransactionSearch
}

enum class ExpandableMenu {
    NOTIFICATION,
    DATA_MANAGEMENT,
    TRANSACTION_TOOLS,
    ANALYSIS,
    IN_APP_PURCHASE
}