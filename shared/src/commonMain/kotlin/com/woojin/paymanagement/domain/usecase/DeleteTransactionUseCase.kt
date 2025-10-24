package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val repository: TransactionRepository,
    private val databaseHelper: DatabaseHelper
) {
    suspend operator fun invoke(transactionId: String) {
        // 삭제할 거래 정보 먼저 조회
        val transaction = repository.getTransactionById(transactionId)

        if (transaction != null) {
            // 1. 잔액권 수입 거래 삭제 시 → 잔액권 삭제
            if (transaction.type == TransactionType.INCOME &&
                transaction.incomeType == IncomeType.BALANCE_CARD &&
                transaction.balanceCardId != null) {
                databaseHelper.deleteBalanceCard(transaction.balanceCardId)
            }

            // 2. 잔액권 지출 거래 삭제 시 → 잔액 복구
            if (transaction.type == TransactionType.EXPENSE &&
                transaction.paymentMethod == PaymentMethod.BALANCE_CARD &&
                transaction.balanceCardId != null) {
                val currentCard = databaseHelper.getBalanceCardById(transaction.balanceCardId)
                if (currentCard != null) {
                    val restoredBalance = currentCard.currentBalance + transaction.amount
                    databaseHelper.updateBalanceCardBalance(
                        id = currentCard.id,
                        currentBalance = restoredBalance,
                        isActive = restoredBalance > 0
                    )
                }
            }

            // 3. 상품권 수입 거래 삭제 시 → 상품권 삭제
            if (transaction.type == TransactionType.INCOME &&
                transaction.incomeType == IncomeType.GIFT_CARD &&
                transaction.giftCardId != null) {
                databaseHelper.deleteGiftCard(transaction.giftCardId)
            }

            // 4. 상품권 지출 거래 삭제 시 → 사용량 복구
            if (transaction.type == TransactionType.EXPENSE &&
                transaction.paymentMethod == PaymentMethod.GIFT_CARD &&
                transaction.giftCardId != null) {
                val currentCard = databaseHelper.getGiftCardById(transaction.giftCardId)
                if (currentCard != null) {
                    val restoredUsedAmount = currentCard.usedAmount - transaction.amount
                    val remainingAmount = currentCard.totalAmount - restoredUsedAmount
                    databaseHelper.updateGiftCardUsage(
                        id = currentCard.id,
                        usedAmount = restoredUsedAmount,
                        isActive = remainingAmount > 0
                    )
                }
            }
        }

        // 거래 삭제
        repository.deleteTransaction(transactionId)
    }
}