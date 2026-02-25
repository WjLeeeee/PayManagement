package com.woojin.paymanagement.presentation.search

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.Transaction

data class SearchUiState(
    val keyword: String = "",
    val results: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false
)
