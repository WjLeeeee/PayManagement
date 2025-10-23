package com.woojin.paymanagement.utils

import platform.Foundation.NSBundle

/**
 * iOS 플랫폼에서 앱 정보를 제공하는 구현체
 */
actual class AppInfo actual constructor() {
    actual fun getVersionName(): String {
        return NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "Unknown"
    }

    actual fun getVersionCode(): Int {
        val versionString = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String
        return versionString?.toIntOrNull() ?: 0
    }
}
