package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.repository.TransactionRepository
import com.woojin.paymanagement.utils.PayPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetPayPeriodTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(payPeriod: PayPeriod): Flow<List<Transaction>> {
        return transactionRepository.getAllTransactions().map { transactions ->
            transactions.filter { transaction ->
                transaction.date >= payPeriod.startDate && transaction.date <= payPeriod.endDate
            }
        }
    }
}