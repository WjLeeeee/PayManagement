package com.woojin.paymanagement.android

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityCompat
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.woojin.paymanagement.android.config.RemoteConfigManager
import com.woojin.paymanagement.android.config.UpdateType
import com.woojin.paymanagement.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.woojin.paymanagement.database.DatabaseDriverFactory
import com.woojin.paymanagement.utils.AppInfo
import com.woojin.paymanagement.utils.FileHandler
import com.woojin.paymanagement.utils.NotificationPermissionChecker
import com.woojin.paymanagement.utils.PreferencesManager
import com.woojin.paymanagement.analytics.Analytics

class MainActivity : ComponentActivity() {

    private var shouldNavigateToParsedTransactions by mutableStateOf(false)
    private var shouldNavigateToRecurringTransactions by mutableStateOf(false)

    // Remote Config 관리자
    private lateinit var remoteConfigManager: RemoteConfigManager

    // 업데이트 상태
    private var updateType by mutableStateOf(UpdateType.NONE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase Analytics 초기화
        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val analyticsLogger = Analytics.getInstance()
        analyticsLogger.initialize(firebaseAnalytics)

        // Firebase Crashlytics 초기화
        val crashlytics = FirebaseCrashlytics.getInstance()
        // 릴리즈 빌드에서만 Crashlytics 활성화
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        // 앱 버전 정보 기록
        crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)

        // 버전 디버그 로그
        val pmVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionCode
        }
        android.util.Log.d("VersionCheck", "BuildConfig.VERSION_CODE=${BuildConfig.VERSION_CODE}, PackageManager.versionCode=$pmVersionCode")

        // Firebase Remote Config 초기화
        remoteConfigManager = RemoteConfigManager()
        CoroutineScope(Dispatchers.IO).launch {
            remoteConfigManager.fetchAndActivate()

            // 업데이트 체크
            val currentVersionCode = BuildConfig.VERSION_CODE
            updateType = remoteConfigManager.checkUpdateType(currentVersionCode)
            android.util.Log.d("RemoteConfig", "updateType = $updateType, currentVersionCode = $currentVersionCode")
        }

        // Intent로부터 네비게이션 플래그 확인
        handleIntent(intent)

        // WorkManager 스케줄링 (매일 오전 9시)
        scheduleRecurringTransactionCheck()

        // AdMob SDK 초기화
        MobileAds.initialize(this) {}

        // PreferencesManager에서 테마 설정 읽기
        val preferencesManager = PreferencesManager(context = this)
        val themeMode = preferencesManager.getThemeMode()

        // 다크 모드 결정 (설정에 따라)
        val systemDarkMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isDarkMode = when (themeMode) {
            com.woojin.paymanagement.utils.ThemeMode.SYSTEM -> systemDarkMode
            com.woojin.paymanagement.utils.ThemeMode.LIGHT -> false
            com.woojin.paymanagement.utils.ThemeMode.DARK -> true
        }

        // 상태바 스타일을 다크모드에 따라 설정
        enableEdgeToEdge(
            statusBarStyle = if (isDarkMode) {
                SystemBarStyle.dark(
                    scrim = Color(0xFF121212).toArgb()
                )
            } else {
                SystemBarStyle.light(
                    scrim = Color.White.toArgb(),
                    darkScrim = Color.White.toArgb()
                )
            },
            navigationBarStyle = if (isDarkMode) {
                SystemBarStyle.dark(
                    scrim = Color(0xFF121212).toArgb()
                )
            } else {
                SystemBarStyle.light(
                    scrim = Color.White.toArgb(),
                    darkScrim = Color.White.toArgb()
                )
            }
        )

        setContent {
            MyApplicationTheme(darkTheme = isDarkMode) {
                StatusBarOverlayScreen(
                    shouldNavigateToParsedTransactions = shouldNavigateToParsedTransactions,
                    shouldNavigateToRecurringTransactions = shouldNavigateToRecurringTransactions,
                    onParsedTransactionsNavigationHandled = {
                        shouldNavigateToParsedTransactions = false
                    },
                    onRecurringTransactionsNavigationHandled = {
                        shouldNavigateToRecurringTransactions = false
                    },
                    onThemeChanged = { recreate() }
                )

                // 업데이트 다이얼로그
                if (updateType != UpdateType.NONE) {
                    UpdateDialog(
                        updateType = updateType,
                        onConfirm = {
                            // Play Store로 이동
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                android.net.Uri.parse("market://details?id=$packageName")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                // Play Store 앱이 없으면 웹으로
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                                    )
                                )
                            }
                            if (updateType == UpdateType.FORCE) {
                                finish()
                            } else {
                                updateType = UpdateType.NONE
                            }
                        },
                        onDismiss = {
                            // 선택적 업데이트만 취소 가능
                            if (updateType == UpdateType.OPTIONAL) {
                                updateType = UpdateType.NONE
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val navigateToParsedTransactions =
            intent.getBooleanExtra(EXTRA_NAVIGATE_TO_PARSED_TRANSACTIONS, false)
        if (navigateToParsedTransactions) {
            shouldNavigateToParsedTransactions = true
        }

        val navigateToRecurringTransactions =
            intent.getBooleanExtra(EXTRA_NAVIGATE_TO_RECURRING_TRANSACTIONS, false)
        if (navigateToRecurringTransactions) {
            shouldNavigateToRecurringTransactions = true
        }
    }

    private fun scheduleRecurringTransactionCheck() {
        // 정기 작업 스케줄링 (매일 오전 9시)
        val workRequest =
            androidx.work.PeriodicWorkRequestBuilder<com.woojin.paymanagement.android.worker.RecurringTransactionWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = java.util.concurrent.TimeUnit.DAYS
            ).setInitialDelay(
                calculateInitialDelayToNineAM(),
                java.util.concurrent.TimeUnit.MILLISECONDS
            ).build()

        androidx.work.WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                "recurring_transaction_check",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

        // 알림 채널 초기화
        com.woojin.paymanagement.android.util.RecurringTransactionNotificationHelper.initialize(this)
    }

    private fun calculateInitialDelayToNineAM(): Long {
        val calendar = java.util.Calendar.getInstance()
        val now = calendar.timeInMillis

        // 오늘 오전 9시 설정
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 9)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        var targetTime = calendar.timeInMillis

        // 이미 오늘 오전 9시가 지났다면 내일 오전 9시로 설정
        if (targetTime <= now) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            targetTime = calendar.timeInMillis
        }

        return targetTime - now
    }

    companion object {
        const val EXTRA_NAVIGATE_TO_PARSED_TRANSACTIONS = "navigate_to_parsed_transactions"
        const val EXTRA_NAVIGATE_TO_RECURRING_TRANSACTIONS = "navigate_to_recurring_transactions"
    }
}

@Composable
fun StatusBarOverlayScreen(
    shouldNavigateToParsedTransactions: Boolean = false,
    shouldNavigateToRecurringTransactions: Boolean = false,
    onParsedTransactionsNavigationHandled: () -> Unit = {},
    onRecurringTransactionsNavigationHandled: () -> Unit = {},
    onThemeChanged: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var permissionResultCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    // 광고 제거 화면으로 이동하기 위한 상태
    var shouldNavigateToAdRemoval by remember { mutableStateOf(false) }

    // 네이티브 광고 상태 관리
    var nativeAdState by remember { mutableStateOf<com.woojin.paymanagement.android.ads.NativeAdState>(com.woojin.paymanagement.android.ads.NativeAdState.Loading) }
    val nativeAdManager = remember { com.woojin.paymanagement.android.ads.NativeAdManager(context) }
    val preferencesManagerForAd = remember { PreferencesManager(context = context) }

    // 광고 미리 로딩 (광고 제거가 활성화되지 않았을 때만)
    LaunchedEffect(Unit) {
        if (!preferencesManagerForAd.isAdRemovalActive()) {
            nativeAdManager.loadAd(
                onAdLoaded = { ad ->
                    nativeAdState = com.woojin.paymanagement.android.ads.NativeAdState.Success(ad)
                },
                onAdFailed = { error ->
                    nativeAdState = com.woojin.paymanagement.android.ads.NativeAdState.Failed
                }
            )
        } else {
            // 광고 제거가 활성화되어 있으면 Failed 상태로 설정 (광고 없이 거래내역만 표시)
            nativeAdState = com.woojin.paymanagement.android.ads.NativeAdState.Failed
        }
    }

    // 정리
    DisposableEffect(Unit) {
        onDispose {
            nativeAdManager.destroy()
        }
    }

    // FileHandler 초기화
    val fileHandler = remember { FileHandler() }

    // 파일 저장 런처
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val data = fileHandler.getPendingSaveData()
                        if (data != null) {
                            outputStream.write(data.second.toByteArray())
                            fileHandler.onSaveSuccess()
                        }
                    }
                } catch (e: Exception) {
                    fileHandler.onSaveError(e.message ?: "파일 저장 실패")
                }
            } ?: fileHandler.onSaveError("파일을 선택하지 않았습니다")
        } else {
            fileHandler.onSaveError("취소되었습니다")
        }
    }

    // 파일 불러오기 런처
    val loadFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val jsonContent = inputStream.bufferedReader().use { it.readText() }
                        fileHandler.onLoadSuccess(jsonContent)
                    }
                } catch (e: Exception) {
                    fileHandler.onLoadError(e.message ?: "파일 불러오기 실패")
                }
            } ?: fileHandler.onLoadError("파일을 선택하지 않았습니다")
        } else {
            fileHandler.onLoadError("취소되었습니다")
        }
    }

    // 알림 권한 요청 런처
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // 권한 요청 결과를 콜백으로 전달
        permissionResultCallback?.invoke(isGranted)
        permissionResultCallback = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 상태바 크기만큼 패딩 추가
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

                // 실제 앱 콘텐츠
                val appInfo = AppInfo().apply {
                    initialize(context)
                }
                val preferencesManager = remember { PreferencesManager(context = context) }
                val billingClient = remember {
                    com.woojin.paymanagement.utils.BillingClient(
                        context = context,
                        activityProvider = { context as? ComponentActivity }
                    )
                }

                // 광고 제거 상태 확인
                val isAdRemovalActive = remember { mutableStateOf(preferencesManager.isAdRemovalActive()) }

                // 화면이 다시 보일 때마다 광고 제거 상태 update
                LaunchedEffect(Unit) {
                    isAdRemovalActive.value = preferencesManager.isAdRemovalActive()
                }

                // Interstitial Ad Manager 생성
                val interstitialAdManager = remember {
                    com.woojin.paymanagement.utils.InterstitialAdManager(
                        context = context,
                        activityProvider = { context as? ComponentActivity },
                        adUnitId = "ca-app-pub-9195598687879551/6004614996"
                    )
                }

                App(
                    modifier = Modifier
                        .weight(1f)
                        .imePadding(),
                    databaseDriverFactory = DatabaseDriverFactory(context = context),
                    preferencesManager = preferencesManager,
                    notificationPermissionChecker = NotificationPermissionChecker(context = context),
                    appInfo = appInfo,
                    fileHandler = fileHandler,
                    billingClient = billingClient,
                    interstitialAdManager = interstitialAdManager,
                    shouldNavigateToParsedTransactions = shouldNavigateToParsedTransactions,
                    shouldNavigateToRecurringTransactions = shouldNavigateToRecurringTransactions,
                    shouldNavigateToAdRemoval = shouldNavigateToAdRemoval,
                    onParsedTransactionsNavigationHandled = onParsedTransactionsNavigationHandled,
                    onRecurringTransactionsNavigationHandled = onRecurringTransactionsNavigationHandled,
                    onAdRemovalNavigationHandled = { shouldNavigateToAdRemoval = false },
                    onThemeChanged = { onThemeChanged() },
                    nativeAdContent = {
                        // 광고 로딩 성공 시에만 표시
                        if (nativeAdState is com.woojin.paymanagement.android.ads.NativeAdState.Success) {
                            val ad = (nativeAdState as com.woojin.paymanagement.android.ads.NativeAdState.Success).ad
                            com.woojin.paymanagement.android.ads.NativeAdItem(nativeAd = ad)
                        }
                    },
                    hasNativeAd = nativeAdState is com.woojin.paymanagement.android.ads.NativeAdState.Success,
                    onRequestPostNotificationPermission = { callback ->
                        // 콜백 저장
                        permissionResultCallback = callback

                        // Android 13 이상에서만 권한 요청
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val activity = context as? ComponentActivity

                            // shouldShowRequestPermissionRationale 확인
                            val shouldShowRationale = activity?.let {
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    it,
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            } ?: false

                            // 권한이 이미 영구적으로 거부되었는지 확인
                            val notificationPermissionChecker =
                                NotificationPermissionChecker(context)
                            val hasPermission =
                                notificationPermissionChecker.hasPostNotificationPermission()

                            if (hasPermission) {
                                // 이미 권한이 있음
                                callback(true)
                                permissionResultCallback = null
                            } else if (shouldShowRationale) {
                                // 권한을 거부한 적이 있지만 다시 물어볼 수 있는 상태
                                // 바로 다이얼로그 표시
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                // 처음 요청하거나, 영구적으로 거부된 상태
                                // 일단 요청해보고, 거부되면 설정으로
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            // Android 13 미만에서는 항상 허용된 것으로 처리
                            callback(true)
                            permissionResultCallback = null
                        }
                    },
                    onLaunchSaveFile = { fileName ->
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/json"
                            putExtra(Intent.EXTRA_TITLE, fileName)
                        }
                        saveFileLauncher.launch(intent)
                    },
                    onLaunchLoadFile = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/json"
                        }
                        loadFileLauncher.launch(intent)
                    },
                    onAppExit = {
                        (context as? ComponentActivity)?.finish()
                    },
                    onContactSupport = {
                        val emailHelper = com.woojin.paymanagement.utils.EmailHelper(context)
                        emailHelper.sendSupportEmail(
                            email = "dldnwls0115@naver.com",
                            subject = "편한 가계부-월급 기반 관리 시스템",
                            appVersion = "${appInfo.getVersionName()}(${appInfo.getVersionCode()})",
                            osVersion = android.os.Build.VERSION.RELEASE,
                            deviceModel = android.os.Build.MODEL
                        )
                    },
                    permissionGuideImage = {
                        Image(
                            painter = painterResource(id = R.drawable.notification_setup_guide),
                            contentDescription = "알림 설정 안내",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                )

                // 하단 배너 광고 (광고 제거가 활성화되지 않았을 때만 표시)
                if (!isAdRemovalActive.value) {
                    BannerAdView(
                        adUnitId = "ca-app-pub-9195598687879551/3919131534",
                        onNavigateToAdRemoval = {
                            shouldNavigateToAdRemoval = true
                        }
                    )
                }

                // 네비게이션 바 영역만큼 여백 추가
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

@Composable
fun UpdateDialog(
    updateType: UpdateType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isForce = updateType == UpdateType.FORCE

    androidx.compose.material3.AlertDialog(
        onDismissRequest = {
            if (!isForce) onDismiss()
        },
        title = {
            androidx.compose.material3.Text(
                text = if (isForce) "업데이트 필요" else "새로운 버전 안내",
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            androidx.compose.material3.Text(
                text = if (isForce)
                    "원활한 서비스 이용을 위해 최신 버전으로 업데이트가 필요합니다."
                else
                    "새로운 버전이 출시되었습니다. 업데이트하시겠습니까?"
            )
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = onConfirm) {
                androidx.compose.material3.Text("업데이트")
            }
        },
        dismissButton = if (!isForce) {
            {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text("나중에")
                }
            }
        } else null
    )
}

