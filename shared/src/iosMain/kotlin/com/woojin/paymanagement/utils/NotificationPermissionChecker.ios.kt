package com.woojin.paymanagement.utils

actual class NotificationPermissionChecker {
    actual fun hasPermission(): Boolean {
        return true // iOS는 알림 리스너 기능 없음
    }

    actual fun openSettings() {
        // iOS는 구현 불필요
    }
}