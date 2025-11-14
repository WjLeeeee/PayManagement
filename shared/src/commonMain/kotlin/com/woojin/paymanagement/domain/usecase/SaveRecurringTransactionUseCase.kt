package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.RecurringTransaction
import com.woojin.paymanagement.domain.repository.RecurringTransactionRepository

class SaveRecurringTransactionUseCase(
    private val repository: RecurringTransactionRepository
) {
    suspend operator fun invoke(recurringTransaction: RecurringTransaction, isUpdate: Boolean = false) {
        if (isUpdate) {
            repository.updateRecurringTransaction(recurringTransaction)
        } else {
            repository.insertRecurringTransaction(recurringTransaction)
        }
    }
}
