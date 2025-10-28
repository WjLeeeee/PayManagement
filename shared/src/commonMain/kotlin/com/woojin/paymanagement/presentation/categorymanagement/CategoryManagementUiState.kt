package com.woojin.paymanagement.presentation.categorymanagement

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.TransactionType

data class CategoryManagementUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
    val isAddDialogVisible: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryEmoji: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
