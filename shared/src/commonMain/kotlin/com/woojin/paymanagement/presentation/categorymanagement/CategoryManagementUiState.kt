package com.woojin.paymanagement.presentation.categorymanagement

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.TransactionType

data class CategoryManagementUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
    // 상위 카테고리 추가
    val isAddDialogVisible: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryEmoji: String = "",
    // 소분류 추가
    val isAddSubDialogVisible: Boolean = false,
    val addSubParentCategory: Category? = null,
    val newSubCategoryName: String = "",
    val newSubCategoryEmoji: String = "",
    // 수정
    val isEditDialogVisible: Boolean = false,
    val editingCategory: Category? = null,
    val editCategoryName: String = "",
    val editCategoryEmoji: String = "",
    val showConfirmDialog: Boolean = false,
    val confirmDialogMessage: String = "",
    val pendingUpdate: (() -> Unit)? = null,
    // 삭제
    val isDeleteDialogVisible: Boolean = false,
    val deletingCategory: Category? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
