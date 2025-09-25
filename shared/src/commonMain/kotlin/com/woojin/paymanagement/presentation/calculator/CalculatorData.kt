package com.woojin.paymanagement.presentation.calculator

import com.woojin.paymanagement.data.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class CalculatorRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val transactionType: TransactionType?,
    val categories: List<String>
)

@Serializable
data class CalculatorResult(
    val totalAmount: Double,
    val transactionCount: Int,
    val averageAmount: Double,
    val categories: List<CategorySummary>
)

@Serializable
data class CategorySummary(
    val category: String,
    val amount: Double,
    val transactionCount: Int,
    val percentage: Double
)