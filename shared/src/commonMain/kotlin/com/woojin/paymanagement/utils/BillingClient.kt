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
 * 광고 제거 상품 ID
 */
enum class AdRemovalProductId(val productId: String) {
    ONE_DAY("adremoval1day"),
    THREE_DAYS("adremoval3days"),
    SEVEN_DAYS("adremoval7days"),
    THIRTY_DAYS("adremoval30days")
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
     * 범용 구매 시작 (광고 제거 등)
     */
    suspend fun launchPurchaseFlow(productId: AdRemovalProductId): BillingResult

    /**
     * 연결 종료
     */
    fun disconnect()
}
