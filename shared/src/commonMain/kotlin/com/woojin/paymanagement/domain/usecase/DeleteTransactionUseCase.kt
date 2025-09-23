package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transactionId: String) {
        repository.deleteTransaction(transactionId)
    }
}