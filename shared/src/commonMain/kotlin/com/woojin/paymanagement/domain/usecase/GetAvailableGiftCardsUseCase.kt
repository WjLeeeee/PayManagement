package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.IncomeType

class GetAvailableGiftCardsUseCase {
    operator fun invoke(transactions: List<Transaction>, activeCardIds: Set<String>? = null): List<GiftCard> {
        // 카드 이름으로 그룹화 (같은 이름의 카드는 하나로 통합)
        val giftCardMap = mutableMapOf<String, GiftCard>()

        // 상품권 수입 거래에서 상품권 생성
        transactions.filter {
            it.type == TransactionType.INCOME &&
            it.incomeType == IncomeType.GIFT_CARD &&
            !it.giftCardId.isNullOrBlank() &&
            !it.cardName.isNullOrBlank()
        }.forEach { transaction ->
            val cardName = transaction.cardName!!
            val cardId = transaction.giftCardId!!

            if (!giftCardMap.containsKey(cardName)) {
                giftCardMap[cardName] = GiftCard(
                    id = cardId, // 첫 번째로 발견된 ID 사용
                    name = cardName,
                    totalAmount = 0.0,
                    usedAmount = 0.0,
                    createdDate = transaction.date,
                    isActive = true
                )
            }

            val currentCard = giftCardMap[cardName]!!
            giftCardMap[cardName] = currentCard.copy(
                totalAmount = currentCard.totalAmount + transaction.amount
            )
        }

        // 상품권 사용 거래에서 사용량 증가
        transactions.filter {
            it.giftCardId != null &&
            it.type == TransactionType.EXPENSE
        }.forEach { transaction ->
            val cardName = transaction.cardName
            if (cardName != null && giftCardMap.containsKey(cardName)) {
                val card = giftCardMap[cardName]!!
                giftCardMap[cardName] = card.copy(
                    usedAmount = card.usedAmount + transaction.amount
                )
            }
        }

        // 잔액이 있는 카드 반환 (activeCardIds가 null이면 필터 없음, 빈 Set이면 전부 차단)
        return giftCardMap.values.filter {
            it.remainingAmount > 0 && (activeCardIds == null || activeCardIds.contains(it.id))
        }
    }
}