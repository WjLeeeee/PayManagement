package com.woojin.paymanagement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.woojin.paymanagement.utils.LifecycleObserverHelper
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
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
    appInfo: com.woojin.paymanagement.utils.AppInfo,
    fileHandler: com.woojin.paymanagement.utils.FileHandler,
    billingClient: com.woojin.paymanagement.utils.BillingClient,
    shouldNavigateToParsedTransactions: Boolean = false,
    onNavigationHandled: () -> Unit = {},
    onSendTestNotifications: ((List<com.woojin.paymanagement.data.ParsedTransaction>) -> Unit)? = null,
    onThemeChanged: (() -> Unit)? = null,
    onRequestPostNotificationPermission: ((onPermissionResult: (Boolean) -> Unit) -> Unit)? = null,
    onLaunchSaveFile: (String) -> Unit = {},
    onLaunchLoadFile: () -> Unit = {}
) {
    var isKoinInitialized by remember { mutableStateOf(false) }

    // Koin 초기화
    LaunchedEffect(Unit) {
        initializeKoin(databaseDriverFactory, preferencesManager, notificationPermissionChecker, appInfo, fileHandler, billingClient)
        isKoinInitialized = true
    }

    MaterialTheme {
        if (isKoinInitialized) {
            PayManagementApp(
                shouldNavigateToParsedTransactions = shouldNavigateToParsedTransactions,
                onNavigationHandled = onNavigationHandled,
                onSendTestNotifications = onSendTestNotifications,
                onThemeChanged = onThemeChanged,
                onRequestPostNotificationPermission = onRequestPostNotificationPermission,
                onLaunchSaveFile = onLaunchSaveFile,
                onLaunchLoadFile = onLaunchLoadFile
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
    notificationPermissionChecker: com.woojin.paymanagement.utils.NotificationPermissionChecker,
    appInfo: com.woojin.paymanagement.utils.AppInfo,
    fileHandler: com.woojin.paymanagement.utils.FileHandler,
    billingClient: com.woojin.paymanagement.utils.BillingClient
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
    onThemeChanged: (() -> Unit)? = null,
    onRequestPostNotificationPermission: ((onPermissionResult: (Boolean) -> Unit) -> Unit)? = null,
    onLaunchSaveFile: (String) -> Unit = {},
    onLaunchLoadFile: () -> Unit = {}
) {
    // DI로 의존성 주입받기
    val preferencesManager: PreferencesManager = koinInject()
    val databaseHelper: DatabaseHelper = koinInject()
    val categoryRepository: com.woojin.paymanagement.domain.repository.CategoryRepository = koinInject()
    val scope = rememberCoroutineScope()

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

    // 앱 정보 다이얼로그 상태
    var showAppInfoDialog by remember { mutableStateOf(false) }

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
    var showListenerPermissionDialog by remember { mutableStateOf(false) }
    var showPostPermissionDialog by remember { mutableStateOf(false) }

    // Deep link 처리: 푸시 알림에서 카드 결제 내역 화면으로 이동
    LaunchedEffect(shouldNavigateToParsedTransactions) {
        if (shouldNavigateToParsedTransactions) {
            // Payday가 설정되어 있을 때만 네비게이션 수행
            if (preferencesManager.isPaydaySet()) {
                navigateTo(Screen.ParsedTransactionList)
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
    var showPaydayChangeDialog by remember { mutableStateOf(false) }

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

    // 월급날 변경 다이얼로그
    if (showPaydayChangeDialog) {
        AlertDialog(
            onDismissRequest = { showPaydayChangeDialog = false },
            title = { Text("월급날 변경") },
            text = {
                Text("월급날을 변경하시겠습니까?\n월급날 설정 화면으로 이동합니다.")
            },
            confirmButton = {
                Button(onClick = {
                    showPaydayChangeDialog = false
                    navigateTo(Screen.PaydaySetup)
                }) {
                    Text("변경하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaydayChangeDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 앱 정보 다이얼로그
    if (showAppInfoDialog) {
        val appInfo = koinInject<com.woojin.paymanagement.utils.AppInfo>()
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            icon = {
                Text(
                    text = "ℹ️",
                    style = MaterialTheme.typography.displayMedium
                )
            },
            title = {
                Text(
                    text = "앱 정보",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "버전 이름: ${appInfo.getVersionName()}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "버전 코드: ${appInfo.getVersionCode()}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppInfoDialog = false }) {
                    Text("확인")
                }
            }
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
                                            text = "월급날 변경",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${preferencesManager.getPayday()}일",
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
                                }
                            )

                            // 푸시 알림 설정 섹션 (확장 가능)
                            val notificationPermissionChecker = koinInject<com.woojin.paymanagement.utils.NotificationPermissionChecker>()
                            var isNotificationExpanded by remember { mutableStateOf(false) }
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
                                            text = "푸시 알림",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = if (isNotificationExpanded)
                                                Icons.Default.KeyboardArrowUp
                                            else
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isNotificationExpanded) "접기" else "펼치기",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    isNotificationExpanded = !isNotificationExpanded
                                },
                                icon = {
                                    Text(
                                        text = "🔔",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            )

                            // 확장된 알림 설정 항목들
                            if (isNotificationExpanded) {
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
                                                text = "카드 알림 감지",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "다른 앱의 카드 알림을 파싱",
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
                                                text = "앱 알림",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "앱에서 알림 받기",
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

                            var isDataManagementExpanded by remember { mutableStateOf(false) }

                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "데이터 관리",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = if (isDataManagementExpanded)
                                                Icons.Default.KeyboardArrowUp
                                            else
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isDataManagementExpanded) "접기" else "펼치기",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    isDataManagementExpanded = !isDataManagementExpanded
                                },
                                icon = {
                                    Text(
                                        text = "💾",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            )

                            // 확장된 데이터 관리 항목들
                            if (isDataManagementExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, top = 4.dp, bottom = 8.dp)
                                ) {
                                    // 데이터 내보내기
                                    val exportDataUseCase = koinInject<com.woojin.paymanagement.domain.usecase.ExportDataUseCase>()
                                    val fileHandler = koinInject<com.woojin.paymanagement.utils.FileHandler>()
                                    var showExportMessage by remember { mutableStateOf<String?>(null) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                scope.launch {
                                                    val result = exportDataUseCase()
                                                    result.onSuccess { jsonString ->
                                                        val fileName = "paymanagement_backup_${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date}.json"
                                                        fileHandler.setSaveData(
                                                            fileName = fileName,
                                                            jsonContent = jsonString,
                                                            onSuccess = {
                                                                showExportMessage = "데이터를 성공적으로 내보냈습니다"
                                                                scope.launch { drawerState.close() }
                                                            },
                                                            onError = { error ->
                                                                showExportMessage = "내보내기 실패: $error"
                                                            }
                                                        )
                                                        onLaunchSaveFile(fileName)
                                                    }.onFailure { error ->
                                                        showExportMessage = "내보내기 실패: ${error.message}"
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
                                                text = "내보내기",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "데이터를 JSON 파일로 저장",
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
                                                text = "가져오기",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "JSON 파일에서 데이터 복원",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // 가져오기 확인 다이얼로그
                                    if (showReplaceConfirmDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showReplaceConfirmDialog = false },
                                            title = { Text("데이터 가져오기") },
                                            text = { Text("⚠️ 기존 데이터를 모두 삭제하고 파일에서 데이터를 가져옵니다.\n\n계속하시겠습니까?") },
                                            confirmButton = {
                                                Button(onClick = {
                                                    showReplaceConfirmDialog = false
                                                    fileHandler.setLoadCallbacks(
                                                        onSuccess = { jsonString ->
                                                            scope.launch {
                                                                val result = importDataUseCase(jsonString, replaceExisting = true)
                                                                result.onSuccess { importResult ->
                                                                    showImportMessage = "데이터 가져오기 완료\n성공: ${importResult.successCount}, 실패: ${importResult.failureCount}"
                                                                    scope.launch { drawerState.close() }
                                                                }.onFailure { error ->
                                                                    showImportMessage = "가져오기 실패: ${error.message}"
                                                                }
                                                            }
                                                        },
                                                        onError = { error ->
                                                            showImportMessage = "파일 불러오기 실패: $error"
                                                        }
                                                    )
                                                    onLaunchLoadFile()
                                                }) {
                                                    Text("가져오기")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showReplaceConfirmDialog = false }) {
                                                    Text("취소")
                                                }
                                            }
                                        )
                                    }

                                    // 내보내기/가져오기 메시지 표시
                                    showExportMessage?.let { message ->
                                        AlertDialog(
                                            onDismissRequest = { showExportMessage = null },
                                            title = { Text("알림") },
                                            text = { Text(message) },
                                            confirmButton = {
                                                Button(onClick = { showExportMessage = null }) {
                                                    Text("확인")
                                                }
                                            }
                                        )
                                    }

                                    showImportMessage?.let { message ->
                                        AlertDialog(
                                            onDismissRequest = { showImportMessage = null },
                                            title = { Text("알림") },
                                            text = { Text(message) },
                                            confirmButton = {
                                                Button(onClick = { showImportMessage = null }) {
                                                    Text("확인")
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 카테고리 관리
                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Text(
                                            text = "카테고리 관리",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "수입/지출 카테고리 추가 및 삭제",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    navigateTo(Screen.CategoryManagement)
                                    scope.launch { drawerState.close() }
                                },
                                icon = {
                                    Text(
                                        text = "📂",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 예산 관리
                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Text(
                                            text = "예산 설정",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "카테고리별 월간 예산 설정",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    navigateTo(Screen.BudgetSettings)
                                    scope.launch { drawerState.close() }
                                },
                                icon = {
                                    Text(
                                        text = "📊",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 잔액권/상품권 관리
                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Text(
                                            text = "잔액권/상품권 관리",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "잔액권 및 상품권 조회 및 삭제",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    navigateTo(Screen.CardManagement)
                                    scope.launch { drawerState.close() }
                                },
                                icon = {
                                    Text(
                                        text = "💳",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 팁주기 (Android만)
                            if (com.woojin.paymanagement.utils.Platform.isAndroid()) {
                                NavigationDrawerItem(
                                    label = {
                                        Column {
                                            Text(
                                                text = "팁주기",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "개발자 응원하기",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    selected = false,
                                    onClick = {
                                        navigateTo(Screen.TipDonation)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = {
                                        Text(
                                            text = "☕",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            val appInfo = koinInject<com.woojin.paymanagement.utils.AppInfo>()
                            NavigationDrawerItem(
                                label = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "앱 정보",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "버전 ${appInfo.getVersionName()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    showAppInfoDialog = true
                                },
                                icon = {
                                    Text(
                                        text = "ℹ️",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
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
                                    text = "테마 설정",
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
                viewModel = addTransactionViewModel,
                onSave = { newTransactions ->
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
                    navigateBack()
                },
                onCancel = {
                    selectedParsedTransaction = null
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
                onSendTestNotifications = onSendTestNotifications,
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

        Screen.TipDonation -> {
            val tipDonationViewModel = remember { koinInject<com.woojin.paymanagement.presentation.tipdonation.TipDonationViewModel>() }

            com.woojin.paymanagement.presentation.tipdonation.TipDonationScreen(
                viewModel = tipDonationViewModel,
                onNavigateBack = { navigateBack() }
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
    TipDonation
}