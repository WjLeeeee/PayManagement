package com.woojin.paymanagement.presentation.categorymanagement

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.TransactionType

data class CategoryManagementUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
    val isAddDialogVisible: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryEmoji: String = "",
    val isEditDialogVisible: Boolean = false,
    val editingCategory: Category? = null,
    val editCategoryName: String = "",
    val editCategoryEmoji: String = "",
    val showConfirmDialog: Boolean = false,
    val confirmDialogMessage: String = "",
    val pendingUpdate: (() -> Unit)? = null,
    val isDeleteDialogVisible: Boolean = false,
    val deletingCategory: Category? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
