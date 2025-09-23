package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.IncomeType

class GetAvailableGiftCardsUseCase {
    operator fun invoke(transactions: List<Transaction>): List<GiftCard> {
        val giftCardMap = mutableMapOf<String, GiftCard>()

        // 상품권 수입 거래에서 상품권 생성
        transactions.filter {
            it.type == TransactionType.INCOME &&
            it.incomeType == IncomeType.GIFT_CARD &&
            !it.giftCardId.isNullOrBlank() &&
            !it.cardName.isNullOrBlank()
        }.forEach { transaction ->
            val cardId = transaction.giftCardId!!
            val cardName = transaction.cardName!!

            if (!giftCardMap.containsKey(cardId)) {
                giftCardMap[cardId] = GiftCard(
                    id = cardId,
                    name = cardName,
                    totalAmount = 0.0,
                    usedAmount = 0.0,
                    createdDate = transaction.date,
                    isActive = true
                )
            }

            val currentCard = giftCardMap[cardId]!!
            giftCardMap[cardId] = currentCard.copy(
                totalAmount = currentCard.totalAmount + transaction.amount
            )
        }

        // 상품권 사용 거래에서 사용량 증가
        transactions.filter {
            it.giftCardId != null &&
            it.type == TransactionType.EXPENSE
        }.forEach { transaction ->
            val cardId = transaction.giftCardId!!
            giftCardMap[cardId]?.let { card ->
                giftCardMap[cardId] = card.copy(
                    usedAmount = card.usedAmount + transaction.amount
                )
            }
        }

        // 잔액이 있는 카드만 반환
        return giftCardMap.values.filter { it.remainingAmount > 0 }
    }
}