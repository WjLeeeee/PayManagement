package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.model.DailySummary

class CalculateDailySummaryUseCase {
    operator fun invoke(transactions: List<Transaction>): DailySummary {
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.displayAmount }

        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.displayAmount }

        val totalSaving = transactions
            .filter { it.type == TransactionType.SAVING }
            .sumOf { it.displayAmount }

        val investmentIncomeCategories = setOf("익절", "배당금")
        val totalInvestment = transactions
            .filter { it.type == TransactionType.INVESTMENT }
            .sumOf { t -> if (t.category in investmentIncomeCategories) t.displayAmount else -t.displayAmount }

        val dailyBalance = totalIncome - totalExpense

        return DailySummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            dailyBalance = dailyBalance,
            totalSaving = totalSaving,
            totalInvestment = totalInvestment
        )
    }
}