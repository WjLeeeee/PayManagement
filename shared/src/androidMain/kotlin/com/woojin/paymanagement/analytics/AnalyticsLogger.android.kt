package com.woojin.paymanagement.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

/**
 * Android용 AnalyticsLogger 구현
 * Firebase Analytics를 사용합니다.
 */
actual class AnalyticsLogger {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    private fun getAnalytics(): FirebaseAnalytics? {
        // Lazy initialization - context가 필요하므로 나중에 초기화
        return firebaseAnalytics
    }

    /**
     * Firebase Analytics 인스턴스를 설정합니다.
     * MainActivity에서 호출됩니다.
     */
    fun initialize(analytics: FirebaseAnalytics) {
        firebaseAnalytics = analytics
    }

    actual fun logScreenView(screenName: String, screenClass: String?) {
        getAnalytics()?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let {
                param(FirebaseAnalytics.Param.SCREEN_CLASS, it)
            }
        }
    }

    actual fun logEvent(eventName: String, params: Map<String, Any>?) {
        getAnalytics()?.logEvent(eventName) {
            params?.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Double -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Boolean -> param(key, if (value) 1L else 0L)
                    else -> param(key, value.toString())
                }
            }
        }
    }

}

/**
 * Analytics 싱글톤 인스턴스 제공
 */
actual object Analytics {
    private var instance: AnalyticsLogger? = null

    actual fun getInstance(): AnalyticsLogger {
        if (instance == null) {
            instance = AnalyticsLogger()
        }
        return instance!!
    }
}
