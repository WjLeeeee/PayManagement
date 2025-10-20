package com.woojin.paymanagement.utils

expect class NotificationPermissionChecker {
    fun hasPermission(): Boolean
    fun openSettings()
    fun hasListenerPermission(): Boolean
    fun hasPostNotificationPermission(): Boolean
    fun openListenerSettings()
    fun openAppNotificationSettings()
}