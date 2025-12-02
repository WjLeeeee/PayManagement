package com.woojin.paymanagement.android.analytics

/**
 * 화면 및 이벤트 추적을 위한 인터페이스
 */
interface AnalyticsTracker {
    /**
     * 화면 조회 이벤트를 로깅합니다.
     * @param screenName 화면 이름
     * @param screenClass 화면 클래스 이름 (선택사항)
     */
    fun logScreenView(screenName: String, screenClass: String? = null)

    /**
     * 커스텀 이벤트를 로깅합니다.
     * @param eventName 이벤트 이름
     * @param params 이벤트 파라미터
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null)
}
