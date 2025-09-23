package com.woojin.paymanagement.domain.model

data class DailySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val dailyBalance: Double
)