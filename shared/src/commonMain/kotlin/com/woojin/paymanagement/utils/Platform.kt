package com.woojin.paymanagement.utils

/**
 * 플랫폼 정보를 제공하는 유틸리티
 */
expect object Platform {
    /**
     * 현재 플랫폼이 Android인지 확인
     */
    fun isAndroid(): Boolean

    /**
     * 현재 플랫폼이 iOS인지 확인
     */
    fun isIOS(): Boolean
}
