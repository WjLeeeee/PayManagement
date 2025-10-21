package com.woojin.paymanagement.android

import android.content.Intent
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

        // 상태바 스타일을 light로 설정 (아이콘 어둡게, 배경 흰색)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = Color.White.toArgb(),
                darkScrim = Color.White.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = Color.White.toArgb(),
                darkScrim = Color.White.toArgb()
            )
        )

        setContent {
            MyApplicationTheme {
                StatusBarOverlayScreen(
                    shouldNavigateToParsedTransactions = shouldNavigateToParsedTransactions,
                    onNavigationHandled = { shouldNavigateToParsedTransactions = false }
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
    onNavigationHandled: () -> Unit = {}
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
                    }
                )
            }
        }
    }
}

