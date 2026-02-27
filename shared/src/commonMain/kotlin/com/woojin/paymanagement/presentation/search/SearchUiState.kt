package com.woojin.paymanagement.presentation.search

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import kotlinx.datetime.LocalDate

data class SearchUiState(
    val keyword: String = "",
    val results: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    // 필터
    val selectedTypes: Set<TransactionType> = emptySet(),
    val selectedCategories: Set<String> = emptySet(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val showFilterSheet: Boolean = false,
    val showDatePicker: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = selectedTypes.isNotEmpty() || selectedCategories.isNotEmpty() ||
                startDate != null || endDate != null

    val activeFilterCount: Int
        get() = listOfNotNull(
            if (selectedTypes.isNotEmpty()) "type" else null,
            if (selectedCategories.isNotEmpty()) "category" else null,
            if (startDate != null || endDate != null) "date" else null
        ).size
}
