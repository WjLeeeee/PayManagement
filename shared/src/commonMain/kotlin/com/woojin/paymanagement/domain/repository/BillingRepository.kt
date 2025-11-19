package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.utils.AdRemovalProductId
import com.woojin.paymanagement.utils.BillingClient
import com.woojin.paymanagement.utils.BillingResult
import com.woojin.paymanagement.utils.TipProductId

/**
 * 결제 관련 Repository
 * Clean Architecture의 Domain Layer에 위치
 */
interface BillingRepository {
    /**
     * 결제 클라이언트 초기화
     */
    fun initialize(onReady: () -> Unit)

    /**
     * 팁 구매
     */
    suspend fun purchaseTip(productId: TipProductId): BillingResult

    /**
     * 광고 제거 구매
     */
    suspend fun launchPurchaseFlow(productId: AdRemovalProductId): BillingResult

    /**
     * 연결 종료
     */
    fun disconnect()
}

/**
 * BillingRepository 구현
 */
class BillingRepositoryImpl(
    private val billingClient: BillingClient
) : BillingRepository {

    override fun initialize(onReady: () -> Unit) {
        billingClient.initialize(onReady)
    }

    override suspend fun purchaseTip(productId: TipProductId): BillingResult {
        return billingClient.purchaseTip(productId)
    }

    override suspend fun launchPurchaseFlow(productId: AdRemovalProductId): BillingResult {
        return billingClient.launchPurchaseFlow(productId)
    }

    override fun disconnect() {
        billingClient.disconnect()
    }
}
