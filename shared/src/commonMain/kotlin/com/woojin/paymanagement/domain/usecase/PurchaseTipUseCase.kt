package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.BillingRepository
import com.woojin.paymanagement.utils.BillingResult
import com.woojin.paymanagement.utils.TipProductId

/**
 * 팁 구매 UseCase
 * Clean Architecture의 Domain Layer에 위치
 *
 * 단일 책임 원칙(SRP): 팁 구매 비즈니스 로직만 담당
 */
class PurchaseTipUseCase(
    private val billingRepository: BillingRepository
) {
    /**
     * 팁 구매 실행
     *
     * @param productId 구매할 상품 ID
     * @return BillingResult 결제 결과
     */
    suspend operator fun invoke(productId: TipProductId): BillingResult {
        return billingRepository.purchaseTip(productId)
    }
}
