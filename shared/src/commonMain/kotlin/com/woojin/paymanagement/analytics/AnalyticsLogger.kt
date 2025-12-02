package com.woojin.paymanagement.analytics

/**
 * 플랫폼별 Analytics 로깅을 위한 공통 인터페이스
 * Android와 iOS에서 각각 구현됩니다.
 */
expect class AnalyticsLogger() {
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

/**
 * Analytics 싱글톤 인스턴스를 제공하는 expect 함수
 */
expect object Analytics {
    fun getInstance(): AnalyticsLogger
}
