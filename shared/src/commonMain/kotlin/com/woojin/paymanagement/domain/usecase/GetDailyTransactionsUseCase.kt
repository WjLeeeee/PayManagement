package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import kotlinx.datetime.LocalDate

class GetDailyTransactionsUseCase {
    operator fun invoke(transactions: List<Transaction>, date: LocalDate): List<Transaction> {
        return transactions.filter { it.date == date }
            .sortedByDescending { it.date }
    }
}