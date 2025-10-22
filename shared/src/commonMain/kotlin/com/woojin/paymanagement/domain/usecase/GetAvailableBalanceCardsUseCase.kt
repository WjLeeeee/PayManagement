package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.IncomeType

class GetAvailableBalanceCardsUseCase {
    operator fun invoke(transactions: List<Transaction>): List<BalanceCard> {
        // 카드 이름으로 그룹화 (같은 이름의 카드는 하나로 통합)
        val balanceCardMap = mutableMapOf<String, BalanceCard>()

        // 잔액권 수입 거래에서 잔액권 생성
        transactions.filter {
            it.type == TransactionType.INCOME &&
            it.incomeType == IncomeType.BALANCE_CARD &&
            !it.balanceCardId.isNullOrBlank() &&
            !it.cardName.isNullOrBlank()
        }.forEach { transaction ->
            val cardName = transaction.cardName!!
            val cardId = transaction.balanceCardId!!

            if (!balanceCardMap.containsKey(cardName)) {
                balanceCardMap[cardName] = BalanceCard(
                    id = cardId, // 첫 번째로 발견된 ID 사용
                    name = cardName,
                    initialAmount = 0.0,
                    currentBalance = 0.0,
                    createdDate = transaction.date,
                    isActive = true
                )
            }

            val currentCard = balanceCardMap[cardName]!!
            balanceCardMap[cardName] = currentCard.copy(
                initialAmount = currentCard.initialAmount + transaction.amount,
                currentBalance = currentCard.currentBalance + transaction.amount
            )
        }

        // 잔액권 사용 거래에서 잔액 차감
        transactions.filter {
            it.balanceCardId != null &&
            it.type == TransactionType.EXPENSE
        }.forEach { transaction ->
            val cardName = transaction.cardName
            if (cardName != null && balanceCardMap.containsKey(cardName)) {
                val card = balanceCardMap[cardName]!!
                balanceCardMap[cardName] = card.copy(
                    currentBalance = card.currentBalance - transaction.amount
                )
            }
        }

        // 잔액이 있는 카드만 반환
        return balanceCardMap.values.filter { it.currentBalance > 0 }
    }
}