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
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.woojin.paymanagement.App
import com.woojin.paymanagement.android.util.TransactionNotificationHelper
import com.woojin.paymanagement.database.DatabaseDriverFactory
import com.woojin.paymanagement.utils.PreferencesManager
import com.woojin.paymanagement.utils.NotificationPermissionChecker
import com.woojin.paymanagement.utils.AppInfo
import com.woojin.paymanagement.utils.FileHandler

class MainActivity : ComponentActivity() {

    private var shouldNavigateToParsedTransactions by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent로부터 네비게이션 플래그 확인
        handleIntent(intent)

        // PreferencesManager에서 테마 설정 읽기
        val preferencesManager = PreferencesManager(context = this)
        val themeMode = preferencesManager.getThemeMode()

        // 다크 모드 결정 (설정에 따라)
        val systemDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
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
                    onNavigationHandled = { shouldNavigateToParsedTransactions = false },
                    onThemeChanged = { recreate() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val navigateToParsedTransactions = intent.getBooleanExtra(EXTRA_NAVIGATE_TO_PARSED_TRANSACTIONS, false)
        if (navigateToParsedTransactions) {
            shouldNavigateToParsedTransactions = true
        }
    }

    companion object {
        const val EXTRA_NAVIGATE_TO_PARSED_TRANSACTIONS = "navigate_to_parsed_transactions"
    }
}

@Composable
fun StatusBarOverlayScreen(
    shouldNavigateToParsedTransactions: Boolean = false,
    onNavigationHandled: () -> Unit = {},
    onThemeChanged: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var permissionResultCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

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
            Column {
                // 상태바 크기만큼 패딩 추가
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

                // 실제 앱 콘텐츠
                val appInfo = AppInfo().apply {
                    initialize(context)
                }
                val billingClient = remember {
                    com.woojin.paymanagement.utils.BillingClient(
                        context = context,
                        activityProvider = { context as? ComponentActivity }
                    )
                }
                App(
                    databaseDriverFactory = DatabaseDriverFactory(context = context),
                    preferencesManager = PreferencesManager(context = context),
                    notificationPermissionChecker = NotificationPermissionChecker(context = context),
                    appInfo = appInfo,
                    fileHandler = fileHandler,
                    billingClient = billingClient,
                    shouldNavigateToParsedTransactions = shouldNavigateToParsedTransactions,
                    onNavigationHandled = onNavigationHandled,
                    onSendTestNotifications = { transactions ->
                        // 각 테스트 거래에 대해 알림 전송
                        transactions.forEach { transaction ->
                            TransactionNotificationHelper.sendTransactionNotification(context, transaction)
                        }
                    },
                    onThemeChanged = { onThemeChanged() },
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
                            val notificationPermissionChecker = NotificationPermissionChecker(context)
                            val hasPermission = notificationPermissionChecker.hasPostNotificationPermission()

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
                    }
                )
            }
        }
    }
}

