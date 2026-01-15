package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.repository.TransactionRepository

class SaveTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val checkBudgetExceededUseCase: CheckBudgetExceededUseCase
) {
    suspend operator fun invoke(transaction: Transaction): BudgetExceededResult? {
        // 거래 저장
        transactionRepository.insertTransaction(transaction)

        // 예산 초과 여부 체크
        return checkBudgetExceededUseCase(transaction)
    }
}