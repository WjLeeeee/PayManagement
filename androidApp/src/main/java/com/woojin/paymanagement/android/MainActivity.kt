package com.woojin.paymanagement.android

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
                App(
                    databaseDriverFactory = DatabaseDriverFactory(context = context),
                    preferencesManager = PreferencesManager(context = context),
                    notificationPermissionChecker = NotificationPermissionChecker(context = context),
                    shouldNavigateToParsedTransactions = shouldNavigateToParsedTransactions,
                    onNavigationHandled = onNavigationHandled,
                    onSendTestNotifications = { transactions ->
                        // 각 테스트 거래에 대해 알림 전송
                        transactions.forEach { transaction ->
                            TransactionNotificationHelper.sendTransactionNotification(context, transaction)
                        }
                    },
                    onThemeChanged = { onThemeChanged() }
                )
            }
        }
    }
}

