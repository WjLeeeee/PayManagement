package com.woojin.paymanagement.presentation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchViewModel(
    private val databaseHelper: DatabaseHelper,
    private val categoryRepository: CategoryRepository,
    private val coroutineScope: CoroutineScope
) {
    var uiState by mutableStateOf(SearchUiState())
        private set

    private var searchJob: Job? = null

    init {
        coroutineScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                uiState = uiState.copy(categories = categories)
            }
        }
    }

    fun onKeywordChange(keyword: String) {
        uiState = uiState.copy(keyword = keyword)
        searchJob?.cancel()
        if (keyword.isBlank()) {
            uiState = uiState.copy(results = emptyList(), isLoading = false)
            return
        }
        searchJob = coroutineScope.launch {
            uiState = uiState.copy(isLoading = true)
            databaseHelper.searchTransactions(keyword).collectLatest { results ->
                uiState = uiState.copy(results = results, isLoading = false)
            }
        }
    }
}
