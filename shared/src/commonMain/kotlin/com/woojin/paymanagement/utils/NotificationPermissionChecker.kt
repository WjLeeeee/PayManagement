package com.woojin.paymanagement.utils

expect class NotificationPermissionChecker {
    fun hasPermission(): Boolean
    fun openSettings()
}