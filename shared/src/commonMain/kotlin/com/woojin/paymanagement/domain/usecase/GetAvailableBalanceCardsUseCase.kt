package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.IncomeType

class GetAvailableBalanceCardsUseCase {
    operator fun invoke(transactions: List<Transaction>): List<BalanceCard> {
        val balanceCardMap = mutableMapOf<String, BalanceCard>()

        // 잔액권 수입 거래에서 잔액권 생성
        transactions.filter {
            it.type == TransactionType.INCOME &&
            it.incomeType == IncomeType.BALANCE_CARD &&
            !it.balanceCardId.isNullOrBlank() &&
            !it.cardName.isNullOrBlank()
        }.forEach { transaction ->
            val cardId = transaction.balanceCardId!!
            val cardName = transaction.cardName!!

            if (!balanceCardMap.containsKey(cardId)) {
                balanceCardMap[cardId] = BalanceCard(
                    id = cardId,
                    name = cardName,
                    initialAmount = 0.0,
                    currentBalance = 0.0,
                    createdDate = transaction.date,
                    isActive = true
                )
            }

            val currentCard = balanceCardMap[cardId]!!
            balanceCardMap[cardId] = currentCard.copy(
                initialAmount = currentCard.initialAmount + transaction.amount,
                currentBalance = currentCard.currentBalance + transaction.amount
            )
        }

        // 잔액권 사용 거래에서 잔액 차감
        transactions.filter {
            it.balanceCardId != null &&
            it.type == TransactionType.EXPENSE
        }.forEach { transaction ->
            val cardId = transaction.balanceCardId!!
            balanceCardMap[cardId]?.let { card ->
                balanceCardMap[cardId] = card.copy(
                    currentBalance = card.currentBalance - transaction.amount
                )
            }
        }

        // 잔액이 있는 카드만 반환
        return balanceCardMap.values.filter { it.currentBalance > 0 }
    }
}