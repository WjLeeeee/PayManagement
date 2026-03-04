package com.woojin.paymanagement.presentation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class SearchViewModel(
    private val databaseHelper: DatabaseHelper,
    private val categoryRepository: CategoryRepository,
    private val coroutineScope: CoroutineScope
) {
    var uiState by mutableStateOf(SearchUiState())
        private set

    private var allTransactions: List<Transaction> = emptyList()

    init {
        coroutineScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                uiState = uiState.copy(categories = categories)
            }
        }
        coroutineScope.launch {
            databaseHelper.getAllTransactions().collect { transactions ->
                allTransactions = transactions
                applySearch()
            }
        }
    }

    fun onKeywordChange(keyword: String) {
        uiState = uiState.copy(keyword = keyword)
        applySearch()
    }

    fun onTypeToggle(type: TransactionType) {
        val updated = uiState.selectedTypes.toMutableSet()
        if (!updated.add(type)) updated.remove(type)
        uiState = uiState.copy(selectedTypes = updated)
        applySearch()
    }

    fun onCategoryToggle(category: String) {
        val updated = uiState.selectedCategories.toMutableSet()
        if (!updated.add(category)) updated.remove(category)
        uiState = uiState.copy(selectedCategories = updated)
        applySearch()
    }

    fun onDateRangeSelected(start: LocalDate, end: LocalDate) {
        uiState = uiState.copy(startDate = start, endDate = end, showDatePicker = false)
        applySearch()
    }

    fun onClearDateRange() {
        uiState = uiState.copy(startDate = null, endDate = null)
        applySearch()
    }

    fun onShowFilterSheet(show: Boolean) {
        uiState = uiState.copy(showFilterSheet = show)
    }

    fun onShowDatePicker(show: Boolean) {
        uiState = uiState.copy(showDatePicker = show)
    }

    fun clearFilters() {
        uiState = uiState.copy(
            selectedTypes = emptySet(),
            selectedCategories = emptySet(),
            startDate = null,
            endDate = null,
            showFilterSheet = false
        )
        applySearch()
    }

    private fun applySearch() {
        val keyword = uiState.keyword.trim()
        if (keyword.isBlank() && !uiState.hasActiveFilters) {
            uiState = uiState.copy(results = emptyList())
            return
        }

        val filtered = allTransactions.filter { tx ->
            val keywordMatch = keyword.isBlank() ||
                (tx.merchant?.contains(keyword, ignoreCase = true) == true) ||
                tx.memo.contains(keyword, ignoreCase = true)

            val typeMatch = uiState.selectedTypes.isEmpty() || tx.type in uiState.selectedTypes

            val categoryMatch = uiState.selectedCategories.isEmpty() ||
                tx.category in uiState.selectedCategories

            val dateMatch = (uiState.startDate == null || tx.date >= uiState.startDate!!) &&
                            (uiState.endDate == null || tx.date <= uiState.endDate!!)

            keywordMatch && typeMatch && categoryMatch && dateMatch
        }

        uiState = uiState.copy(results = filtered)
    }
}
