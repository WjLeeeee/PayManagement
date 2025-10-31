package com.woojin.paymanagement.utils

/**
 * 인앱 결제 결과
 */
sealed class BillingResult {
    data object Success : BillingResult()
    data class Error(val message: String) : BillingResult()
    data object Canceled : BillingResult()
}

/**
 * 팁 상품 ID
 */
enum class TipProductId(val productId: String) {
    COFFEE("coffeetip"),
    LUNCH("lunchtip"),
    DINNER("dinnertip")
}

/**
 * 플랫폼별 인앱 결제 클라이언트
 */
expect class BillingClient {
    /**
     * 결제 초기화
     */
    fun initialize(onReady: () -> Unit)

    /**
     * 팁 구매 시작
     */
    suspend fun purchaseTip(productId: TipProductId): BillingResult

    /**
     * 연결 종료
     */
    fun disconnect()
}
