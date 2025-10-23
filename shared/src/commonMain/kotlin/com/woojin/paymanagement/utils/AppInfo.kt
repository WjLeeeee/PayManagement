package com.woojin.paymanagement.utils

/**
 * 플랫폼별 앱 정보를 제공하는 인터페이스
 */
expect class AppInfo() {
    /**
     * 앱 버전 이름을 반환합니다 (예: "1.0.0")
     */
    fun getVersionName(): String

    /**
     * 앱 버전 코드를 반환합니다 (예: 1)
     */
    fun getVersionCode(): Int
}
