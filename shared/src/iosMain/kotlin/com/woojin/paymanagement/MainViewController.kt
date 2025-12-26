package com.woojin.paymanagement

import androidx.compose.ui.window.ComposeUIViewController
import com.woojin.paymanagement.database.DatabaseDriverFactory
import com.woojin.paymanagement.utils.PreferencesManager
import com.woojin.paymanagement.utils.NotificationPermissionChecker
import platform.UIKit.UIDevice

fun MainViewController() = ComposeUIViewController {
    val appInfo = com.woojin.paymanagement.utils.AppInfo()
    val emailHelper = com.woojin.paymanagement.utils.EmailHelper()

    App(
        databaseDriverFactory = DatabaseDriverFactory(),
        preferencesManager = PreferencesManager(),
        notificationPermissionChecker = NotificationPermissionChecker(),
        appInfo = appInfo,
        fileHandler = com.woojin.paymanagement.utils.FileHandler(),
        billingClient = com.woojin.paymanagement.utils.BillingClient(),
        onThemeChanged = {
            // iOS에서는 앱 재시작 대신 다른 방식으로 처리 필요
            // 현재는 앱을 다시 시작해야 테마가 적용됨
        },
        onContactSupport = {
            emailHelper.sendSupportEmail(
                email = "dldnwls0115@naver.com",
                subject = "편한 가계부-월급 기반 관리 시스템",
                appVersion = "${appInfo.getVersionName()}(${appInfo.getVersionCode()})",
                osVersion = UIDevice.currentDevice.systemVersion,
                deviceModel = UIDevice.currentDevice.model
            )
        }
    )
}