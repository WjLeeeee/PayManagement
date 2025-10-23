package com.woojin.paymanagement.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build

/**
 * Android 플랫폼에서 앱 정보를 제공하는 구현체
 */
actual class AppInfo actual constructor() {
    private var _context: Context? = null

    // Context를 설정하는 메서드 (Koin에서 주입 받을 때 사용)
    fun initialize(context: Context) {
        _context = context
    }

    actual fun getVersionName(): String {
        return try {
            val context = _context ?: return "Unknown"
            val packageInfo = getPackageInfo(context)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    actual fun getVersionCode(): Int {
        return try {
            val context = _context ?: return 0
            val packageInfo = getPackageInfo(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun getPackageInfo(context: Context): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    }
}
