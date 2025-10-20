package com.woojin.paymanagement.utils

actual class NotificationPermissionChecker {
    actual fun hasPermission(): Boolean {
        return true // iOS는 알림 리스너 기능 없음
    }

    actual fun openSettings() {
        // iOS는 구현 불필요
    }

    actual fun hasListenerPermission(): Boolean {
        return true // iOS는 알림 리스너 기능 없음
    }

    actual fun hasPostNotificationPermission(): Boolean {
        return true // iOS는 알림 전송 권한이 별도로 필요 없음
    }

    actual fun openListenerSettings() {
        // iOS는 구현 불필요
    }

    actual fun openAppNotificationSettings() {
        // iOS는 구현 불필요
    }
}