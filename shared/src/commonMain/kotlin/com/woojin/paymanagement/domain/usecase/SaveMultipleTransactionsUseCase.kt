package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.repository.TransactionRepository

class SaveMultipleTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transactions: List<Transaction>) {
        transactions.forEach { transaction ->
            transactionRepository.insertTransaction(transaction)
        }
    }
}