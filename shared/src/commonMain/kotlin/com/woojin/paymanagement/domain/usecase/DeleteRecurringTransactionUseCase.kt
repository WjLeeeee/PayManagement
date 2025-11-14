package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.RecurringTransactionRepository

class DeleteRecurringTransactionUseCase(
    private val repository: RecurringTransactionRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteRecurringTransaction(id)
    }
}
