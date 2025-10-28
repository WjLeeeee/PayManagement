package com.woojin.paymanagement.presentation.categorymanagement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.usecase.GetCategoriesUseCase
import com.woojin.paymanagement.domain.usecase.AddCategoryUseCase
import com.woojin.paymanagement.domain.usecase.DeleteCategoryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

class CategoryManagementViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    var uiState by mutableStateOf(CategoryManagementUiState())
        private set

    private var categoriesJob: Job? = null

    init {
        loadCategories()
    }

    fun selectType(type: TransactionType) {
        uiState = uiState.copy(selectedType = type)
        loadCategories()
    }

    private fun loadCategories() {
        // 이전 collect Job 취소
        categoriesJob?.cancel()

        // 새로운 collect Job 시작
        categoriesJob = viewModelScope.launch {
            getCategoriesUseCase(uiState.selectedType).collect { categories ->
                uiState = uiState.copy(categories = categories)
            }
        }
    }

    fun showAddDialog() {
        uiState = uiState.copy(
            isAddDialogVisible = true,
            newCategoryName = "",
            newCategoryEmoji = ""
        )
    }

    fun hideAddDialog() {
        uiState = uiState.copy(isAddDialogVisible = false)
    }

    fun updateNewCategoryName(name: String) {
        uiState = uiState.copy(newCategoryName = name)
    }

    fun updateNewCategoryEmoji(emoji: String) {
        uiState = uiState.copy(newCategoryEmoji = emoji)
    }

    fun addCategory() {
        if (uiState.newCategoryName.isBlank() || uiState.newCategoryEmoji.isBlank()) {
            return
        }

        viewModelScope.launch {
            try {
                val newCategory = Category(
                    id = UUID.randomUUID().toString(),
                    name = uiState.newCategoryName.trim(),
                    emoji = uiState.newCategoryEmoji.trim(),
                    type = uiState.selectedType,
                    sortOrder = uiState.categories.size
                )
                addCategoryUseCase(newCategory)
                hideAddDialog()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                deleteCategoryUseCase(categoryId)
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}
