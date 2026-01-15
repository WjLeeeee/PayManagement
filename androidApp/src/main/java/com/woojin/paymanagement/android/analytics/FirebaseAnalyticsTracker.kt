package com.woojin.paymanagement.android.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

/**
 * Firebase Analytics를 활용한 AnalyticsTracker 구현
 */
class FirebaseAnalyticsTracker(
    context: Context
) : AnalyticsTracker {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun logScreenView(screenName: String, screenClass: String?) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let {
                param(FirebaseAnalytics.Param.SCREEN_CLASS, it)
            }
        }
    }

    override fun logEvent(eventName: String, params: Map<String, Any>?) {
        firebaseAnalytics.logEvent(eventName) {
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
