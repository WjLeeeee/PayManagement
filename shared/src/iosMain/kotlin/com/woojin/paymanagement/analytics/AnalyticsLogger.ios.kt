package com.woojin.paymanagement.analytics

/**
 * iOS용 AnalyticsLogger 구현
 * TODO: Firebase Analytics iOS SDK 연동 필요
 */
actual class AnalyticsLogger {
    actual fun logScreenView(screenName: String, screenClass: String?) {
        // iOS에서는 아직 구현되지 않음
        println("iOS Analytics - Screen View: $screenName")
    }

    actual fun logEvent(eventName: String, params: Map<String, Any>?) {
        // iOS에서는 아직 구현되지 않음
        println("iOS Analytics - Event: $eventName, Params: $params")
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
