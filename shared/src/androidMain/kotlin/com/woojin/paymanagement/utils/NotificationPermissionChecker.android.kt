package com.woojin.paymanagement.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

actual class NotificationPermissionChecker(private val context: Context) {
    actual fun hasPermission(): Boolean {
        // 알림 리스너 권한 체크
        val hasListenerPermission = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

        // 알림 전송 권한 체크 (Android 13 이상)
        val hasPostPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 미만에서는 알림 전송 권한이 필요 없음
        }

        return hasListenerPermission && hasPostPermission
    }

    actual fun openSettings() {
        openListenerSettings()
    }

    /**
     * 알림 리스너 설정 화면 열기
     */
    actual fun openListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 앱 알림 설정 화면 열기
     */
    actual fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * 알림 전송 권한이 있는지 체크 (Android 13 이상)
     */
    actual fun hasPostNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 알림 리스너 권한이 있는지 체크
     */
    actual fun hasListenerPermission(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }
}