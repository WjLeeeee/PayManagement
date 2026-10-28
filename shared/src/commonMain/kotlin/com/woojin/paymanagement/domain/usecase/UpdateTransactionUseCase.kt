package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.TransactionRepository

class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val databaseHelper: DatabaseHelper
) {
    suspend operator fun invoke(transaction: Transaction) {
        // 이전 거래 정보 가져오기
        val oldTransaction = transactionRepository.getTransactionById(transaction.id)

        // 이전 거래가 있는 경우에만 잔액 조정 처리
        if (oldTransaction != null) {
            // 1. 이전 결제수단의 잔액 복원
            restoreBalanceFromOldTransaction(oldTransaction)

            // 2. 새 결제수단의 잔액 차감
            deductBalanceFromNewTransaction(transaction)

            // 잔액권을 처음 만든 충전 거래의 금액·날짜를 고치면 카드 초기 정보도 맞춤 (삭제 시 '처음 만든 거래' 판별에 쓰임)
            if (oldTransaction.isBalanceCardCharge() && transaction.isBalanceCardCharge() &&
                oldTransaction.balanceCardId == transaction.balanceCardId) {
                val card = databaseHelper.getBalanceCardById(oldTransaction.balanceCardId!!)
                if (card != null && card.createdDate == oldTransaction.date && card.initialAmount == oldTransaction.amount) {
                    databaseHelper.updateBalanceCard(card.copy(initialAmount = transaction.amount, createdDate = transaction.date))
                }
            }
        }

        // 3. 거래 정보 업데이트
        transactionRepository.updateTransaction(transaction)
    }

    /**
     * 이전 거래의 결제수단에 따라 잔액을 복원합니다.
     */
    private suspend fun restoreBalanceFromOldTransaction(oldTransaction: Transaction) {
        when {
            // 이전에 잔액권 충전(수입)이었던 경우 → 충전액만큼 잔액에서 뺌
            oldTransaction.isBalanceCardCharge() -> {
                val balanceCard = databaseHelper.getBalanceCardById(oldTransaction.balanceCardId!!)
                if (balanceCard != null) {
                    val restoredBalance = balanceCard.currentBalance - oldTransaction.amount
                    databaseHelper.updateBalanceCardBalance(
                        id = balanceCard.id,
                        currentBalance = restoredBalance,
                        isActive = restoredBalance > 0
                    )
                }
            }

            // 이전에 잔액권으로 지출한 경우 → 잔액 복원
            oldTransaction.type == TransactionType.EXPENSE &&
            oldTransaction.paymentMethod == PaymentMethod.BALANCE_CARD &&
            oldTransaction.balanceCardId != null -> {
                val balanceCard = databaseHelper.getBalanceCardById(oldTransaction.balanceCardId)
                if (balanceCard != null) {
                    val restoredBalance = balanceCard.currentBalance + oldTransaction.amount
                    databaseHelper.updateBalanceCardBalance(
                        id = balanceCard.id,
                        currentBalance = restoredBalance,
                        isActive = restoredBalance > 0
                    )
                }
            }
        }
    }

    /**
     * 새 거래의 결제수단에 따라 잔액을 차감합니다.
     */
    private suspend fun deductBalanceFromNewTransaction(newTransaction: Transaction) {
        when {
            // 잔액권 충전(수입)인 경우 → 충전액만큼 잔액에 더함
            newTransaction.isBalanceCardCharge() -> {
                val balanceCard = databaseHelper.getBalanceCardById(newTransaction.balanceCardId!!)
                if (balanceCard != null) {
                    val newBalance = balanceCard.currentBalance + newTransaction.amount
                    databaseHelper.updateBalanceCardBalance(
                        id = balanceCard.id,
                        currentBalance = newBalance,
                        isActive = newBalance > 0
                    )
                }
            }

            // 새로 잔액권으로 지출하는 경우 → 잔액 차감
            newTransaction.type == TransactionType.EXPENSE &&
            newTransaction.paymentMethod == PaymentMethod.BALANCE_CARD &&
            newTransaction.balanceCardId != null -> {
                val balanceCard = databaseHelper.getBalanceCardById(newTransaction.balanceCardId)
                if (balanceCard != null) {
                    val newBalance = balanceCard.currentBalance - newTransaction.amount
                    databaseHelper.updateBalanceCardBalance(
                        id = balanceCard.id,
                        currentBalance = newBalance,
                        isActive = newBalance > 0
                    )
                }
            }
        }
    }

    private fun Transaction.isBalanceCardCharge() =
        type == TransactionType.INCOME && incomeType == IncomeType.BALANCE_CARD && balanceCardId != null
}
