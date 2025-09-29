package com.woojin.paymanagement.domain.usecase.card

import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.repository.TransactionRepository

/**
 * 잔액권 관리 비즈니스 로직을 담당하는 UseCase
 * SRP: 잔액권 관련 비즈니스 로직만 담당
 * DIP: Repository 인터페이스에 의존
 */
class ManageBalanceCardUseCase(
    private val transactionRepository: TransactionRepository
) {

    /**
     * 잔액권 수입 거래 시 잔액권 생성
     */
    suspend fun createBalanceCardFromTransaction(transaction: Transaction): Result<Unit> {
        return try {
            if (!isBalanceCardIncomeTransaction(transaction)) {
                return Result.failure(IllegalArgumentException("잔액권 수입 거래가 아닙니다"))
            }

            val balanceCard = createBalanceCardFromIncomeTransaction(transaction)
            transactionRepository.insertBalanceCard(balanceCard)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 잔액권 지출 거래 시 잔액권 잔액 업데이트
     */
    suspend fun updateBalanceCardFromExpense(transaction: Transaction): Result<Unit> {
        return try {
            if (!isBalanceCardExpenseTransaction(transaction)) {
                return Result.failure(IllegalArgumentException("잔액권 지출 거래가 아닙니다"))
            }

            val balanceCardId = transaction.balanceCardId
                ?: return Result.failure(IllegalArgumentException("잔액권 ID가 없습니다"))

            val currentCard = transactionRepository.getBalanceCardById(balanceCardId)
                ?: return Result.failure(IllegalArgumentException("잔액권을 찾을 수 없습니다"))

            val newBalance = currentCard.currentBalance - transaction.amount
            val isActive = newBalance > 0

            transactionRepository.updateBalanceCardBalance(
                id = currentCard.id,
                currentBalance = newBalance,
                isActive = isActive
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 잔액권 수입 거래인지 확인
     */
    private fun isBalanceCardIncomeTransaction(transaction: Transaction): Boolean {
        return transaction.type == TransactionType.INCOME &&
                transaction.incomeType == IncomeType.BALANCE_CARD &&
                transaction.balanceCardId != null &&
                transaction.cardName != null
    }

    /**
     * 잔액권 지출 거래인지 확인
     */
    private fun isBalanceCardExpenseTransaction(transaction: Transaction): Boolean {
        return transaction.type == TransactionType.EXPENSE &&
                transaction.paymentMethod == PaymentMethod.BALANCE_CARD &&
                transaction.balanceCardId != null
    }

    /**
     * 수입 거래로부터 잔액권 엔티티 생성
     */
    private fun createBalanceCardFromIncomeTransaction(transaction: Transaction): BalanceCard {
        return BalanceCard(
            id = requireNotNull(transaction.balanceCardId) { "잔액권 ID가 필요합니다" },
            name = requireNotNull(transaction.cardName) { "잔액권 이름이 필요합니다" },
            initialAmount = transaction.amount,
            currentBalance = transaction.amount,
            createdDate = transaction.date,
            isActive = true
        )
    }
}