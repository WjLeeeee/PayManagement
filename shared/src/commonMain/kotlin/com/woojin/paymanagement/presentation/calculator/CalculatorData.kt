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
    val categories: List<CategorySummary>,
    val transactionDetails: List<TransactionDetail>
)

@Serializable
data class CategorySummary(
    val category: String,
    val amount: Double,
    val transactionCount: Int,
    val percentage: Double
)

@Serializable
data class TransactionDetail(
    val amount: Double,
    val memo: String,
    val merchant: String? = null,  // 사용처
    val date: LocalDate
)