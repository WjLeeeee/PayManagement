package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.presentation.calendar.PayPeriodSummary
import com.woojin.paymanagement.utils.PayPeriod

class GetPayPeriodSummaryUseCase {
    operator fun invoke(transactions: List<Transaction>, payPeriod: PayPeriod): PayPeriodSummary {
        val periodTransactions = transactions.filter { transaction ->
            transaction.date >= payPeriod.startDate && transaction.date <= payPeriod.endDate
        }

        val totalIncome = periodTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpense = periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        return PayPeriodSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = totalIncome - totalExpense,
            transactionCount = periodTransactions.size
        )
    }
}