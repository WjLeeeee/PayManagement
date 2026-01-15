package com.woojin.paymanagement.utils

/**
 * iOS 인앱 결제 클라이언트 (빈 구현)
 * iOS에서는 팁주기 메뉴가 표시되지 않으므로 사용되지 않습니다
 */
actual class BillingClient {
    /**
     * 결제 초기화
     */
    actual fun initialize(onReady: () -> Unit) {
        // iOS는 지원하지 않음
        onReady()
    }

    /**
     * 팁 구매 시작
     */
    actual suspend fun purchaseTip(productId: TipProductId): BillingResult {
        return BillingResult.Error("iOS에서는 지원하지 않습니다")
    }

    /**
     * 광고 제거 구매 시작
     */
    actual suspend fun launchPurchaseFlow(productId: AdRemovalProductId): BillingResult {
        return BillingResult.Error("iOS에서는 지원하지 않습니다")
    }

    /**
     * 연결 종료
     */
    actual fun disconnect() {
        // iOS는 지원하지 않음
    }
}
