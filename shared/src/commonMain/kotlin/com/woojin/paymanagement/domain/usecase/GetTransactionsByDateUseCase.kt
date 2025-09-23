package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class GetTransactionsByDateUseCase(
    private val repository: TransactionRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<Transaction>> {
        return repository.getAllTransactions().map { transactions ->
            transactions.filter { it.date == date }
        }
    }
}