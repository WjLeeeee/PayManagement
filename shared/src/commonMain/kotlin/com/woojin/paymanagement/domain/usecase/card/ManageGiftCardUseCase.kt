package com.woojin.paymanagement.domain.usecase.card

import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.repository.TransactionRepository

/**
 * 상품권 관리 비즈니스 로직을 담당하는 UseCase
 * SRP: 상품권 관련 비즈니스 로직만 담당
 * DIP: Repository 인터페이스에 의존
 */
class ManageGiftCardUseCase(
    private val transactionRepository: TransactionRepository
) {

    /**
     * 상품권 수입 거래 시 상품권 생성
     */
    suspend fun createGiftCardFromTransaction(transaction: Transaction): Result<Unit> {
        return try {
            if (!isGiftCardIncomeTransaction(transaction)) {
                return Result.failure(IllegalArgumentException("상품권 수입 거래가 아닙니다"))
            }

            val giftCard = createGiftCardFromIncomeTransaction(transaction)
            transactionRepository.insertGiftCard(giftCard)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 상품권 지출 거래 시 상품권 사용량 업데이트
     * 상품권은 한 번 사용되면 완전히 비활성화됨 (환급 발생)
     */
    suspend fun updateGiftCardFromExpense(transaction: Transaction): Result<Unit> {
        return try {
            if (!isGiftCardExpenseTransaction(transaction)) {
                return Result.failure(IllegalArgumentException("상품권 지출 거래가 아닙니다"))
            }

            val giftCardId = transaction.giftCardId
                ?: return Result.failure(IllegalArgumentException("상품권 ID가 없습니다"))

            val currentCard = transactionRepository.getGiftCardById(giftCardId)
                ?: return Result.failure(IllegalArgumentException("상품권을 찾을 수 없습니다"))

            val newUsedAmount = currentCard.usedAmount + transaction.amount

            transactionRepository.updateGiftCardUsage(
                id = currentCard.id,
                usedAmount = newUsedAmount,
                isActive = false // 한 번 사용되면 완전히 비활성화
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 상품권 수입 거래인지 확인
     */
    private fun isGiftCardIncomeTransaction(transaction: Transaction): Boolean {
        return transaction.type == TransactionType.INCOME &&
                transaction.incomeType == IncomeType.GIFT_CARD &&
                transaction.giftCardId != null &&
                transaction.cardName != null
    }

    /**
     * 상품권 지출 거래인지 확인
     */
    private fun isGiftCardExpenseTransaction(transaction: Transaction): Boolean {
        return transaction.type == TransactionType.EXPENSE &&
                transaction.paymentMethod == PaymentMethod.GIFT_CARD &&
                transaction.giftCardId != null
    }

    /**
     * 수입 거래로부터 상품권 엔티티 생성
     */
    private fun createGiftCardFromIncomeTransaction(transaction: Transaction): GiftCard {
        return GiftCard(
            id = requireNotNull(transaction.giftCardId) { "상품권 ID가 필요합니다" },
            name = requireNotNull(transaction.cardName) { "상품권 이름이 필요합니다" },
            totalAmount = transaction.amount,
            usedAmount = 0.0,
            createdDate = transaction.date,
            isActive = true,
            minimumUsageRate = 0.0 // 기본값
        )
    }
}