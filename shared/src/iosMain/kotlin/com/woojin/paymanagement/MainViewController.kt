package com.woojin.paymanagement

import androidx.compose.ui.window.ComposeUIViewController
import com.woojin.paymanagement.database.DatabaseDriverFactory
import com.woojin.paymanagement.utils.PreferencesManager
import com.woojin.paymanagement.utils.NotificationPermissionChecker

fun MainViewController() = ComposeUIViewController {
    App(
        databaseDriverFactory = DatabaseDriverFactory(),
        preferencesManager = PreferencesManager(),
        notificationPermissionChecker = NotificationPermissionChecker(),
        appInfo = com.woojin.paymanagement.utils.AppInfo(),
        fileHandler = com.woojin.paymanagement.utils.FileHandler(),
        billingClient = com.woojin.paymanagement.utils.BillingClient(),
        onThemeChanged = {
            // iOS에서는 앱 재시작 대신 다른 방식으로 처리 필요
            // 현재는 앱을 다시 시작해야 테마가 적용됨
        }
    )
}