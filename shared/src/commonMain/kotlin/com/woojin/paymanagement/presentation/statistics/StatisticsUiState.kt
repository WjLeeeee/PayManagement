package com.woojin.paymanagement.presentation.statistics

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.ChartData
import com.woojin.paymanagement.data.PaymentMethodSummary
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.utils.PayPeriod

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPayPeriod: PayPeriod? = null,
    val transactions: List<Transaction> = emptyList(),
    val chartData: ChartData? = null,
    val paymentSummary: PaymentMethodSummary? = null,
    val showCalculatorDialog: Boolean = false,
    val availableCategories: List<Category> = emptyList()
)