package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.model.DailySummary

class CalculateDailySummaryUseCase {
    operator fun invoke(transactions: List<Transaction>): DailySummary {
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val dailyBalance = totalIncome - totalExpense

        return DailySummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            dailyBalance = dailyBalance
        )
    }
}