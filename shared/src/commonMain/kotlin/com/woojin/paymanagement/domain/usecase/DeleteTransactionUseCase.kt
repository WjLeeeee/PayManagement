package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.TransactionRepository

data class DeletionValidationResult(
    val canDelete: Boolean,
    val errorMessage: String? = null
)

class DeleteTransactionUseCase(
    private val repository: TransactionRepository,
    private val databaseHelper: DatabaseHelper
) {
    // 삭제 가능 여부를 먼저 검증
    suspend fun validateDeletion(transactionId: String): DeletionValidationResult {
        val transaction = repository.getTransactionById(transactionId) ?: return DeletionValidationResult(true)

        // 잔액권 수입 거래 삭제 검증
        if (transaction.type == TransactionType.INCOME &&
            transaction.incomeType == IncomeType.BALANCE_CARD &&
            transaction.balanceCardId != null) {

            val currentCard = databaseHelper.getBalanceCardById(transaction.balanceCardId)
            if (currentCard != null) {
                val expectedBalance = currentCard.currentBalance - transaction.amount
                val totalExpense = databaseHelper.getTotalExpenseByBalanceCard(transaction.balanceCardId)

                if (expectedBalance < totalExpense) {
                    return DeletionValidationResult(
                        canDelete = false,
                        errorMessage = "이 거래를 삭제하면 잔액이 부족해집니다.\n" +
                                "삭제 후 잔액: ${expectedBalance.toInt()}원\n" +
                                "사용한 금액: ${totalExpense.toInt()}원"
                    )
                }
            }
        }

        return DeletionValidationResult(canDelete = true)
    }

    suspend operator fun invoke(transactionId: String) {
        // 삭제할 거래 정보 먼저 조회
        val transaction = repository.getTransactionById(transactionId)

        if (transaction != null) {
            // 1. 잔액권 수입 거래 삭제 시 처리
            if (transaction.type == TransactionType.INCOME &&
                transaction.incomeType == IncomeType.BALANCE_CARD &&
                transaction.balanceCardId != null) {

                val currentCard = databaseHelper.getBalanceCardById(transaction.balanceCardId)
                if (currentCard != null) {
                    val expectedBalance = currentCard.currentBalance - transaction.amount

                    // 새 잔액권 생성 거래인지 확인
                    val isInitialTransaction = currentCard.createdDate == transaction.date &&
                                               currentCard.initialAmount == transaction.amount

                    if (isInitialTransaction) {
                        // 최초 생성 거래 → 잔액권 삭제
                        databaseHelper.deleteBalanceCard(transaction.balanceCardId)
                    } else {
                        // 충전 거래 → 금액만 차감
                        databaseHelper.updateBalanceCardBalance(
                            id = currentCard.id,
                            currentBalance = expectedBalance,
                            isActive = expectedBalance > 0
                        )
                    }
                }
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