package com.woojin.paymanagement.android.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object NotificationPermissionHelper {

    fun hasNotificationPermission(context: Context): Boolean {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledListeners.contains(context.packageName)
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}