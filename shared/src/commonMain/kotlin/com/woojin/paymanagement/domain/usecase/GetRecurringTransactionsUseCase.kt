package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.RecurringTransaction
import com.woojin.paymanagement.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow

class GetRecurringTransactionsUseCase(
    private val repository: RecurringTransactionRepository
) {
    operator fun invoke(activeOnly: Boolean = false): Flow<List<RecurringTransaction>> {
        return if (activeOnly) {
            repository.getActiveRecurringTransactions()
        } else {
            repository.getAllRecurringTransactions()
        }
    }
}
