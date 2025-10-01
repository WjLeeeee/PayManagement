package com.woojin.paymanagement.presentation.calendar

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.utils.PayPeriod
import kotlinx.datetime.LocalDate

data class CalendarUiState(
    val isLoading: Boolean = false,
    val currentPayPeriod: PayPeriod? = null,
    val selectedDate: LocalDate? = null,
    val transactions: List<Transaction> = emptyList(),
    val payPeriodSummary: PayPeriodSummary = PayPeriodSummary(),
    val dailyTransactions: List<Transaction> = emptyList(),
    val isMoneyVisible: Boolean = true,
    val error: String? = null
)

data class PayPeriodSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val transactionCount: Int = 0
)