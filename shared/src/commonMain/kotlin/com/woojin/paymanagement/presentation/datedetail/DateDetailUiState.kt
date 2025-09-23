package com.woojin.paymanagement.presentation.datedetail

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.model.DailySummary
import kotlinx.datetime.LocalDate

data class DateDetailUiState(
    val selectedDate: LocalDate? = null,
    val transactions: List<Transaction> = emptyList(),
    val dailySummary: DailySummary = DailySummary(0.0, 0.0, 0.0),
    val isLoading: Boolean = false,
    val error: String? = null
)