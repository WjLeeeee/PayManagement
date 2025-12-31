package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.repository.TransactionRepository

class SaveMultipleTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val checkBudgetExceededUseCase: CheckBudgetExceededUseCase
) {
    suspend operator fun invoke(transactions: List<Transaction>): BudgetExceededResult? {
        var budgetExceededResult: BudgetExceededResult? = null

        transactions.forEach { transaction ->
            transactionRepository.insertTransaction(transaction)

            // 각 거래에 대해 예산 체크
            val result = checkBudgetExceededUseCase(transaction)

            // 가장 높은 임계값의 결과를 저장 (100% > 70%)
            if (result != null) {
                if (budgetExceededResult == null || result.threshold > budgetExceededResult!!.threshold) {
                    budgetExceededResult = result
                }
            }
        }

        return budgetExceededResult
    }
}