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
import com.benasher44.uuid.uuid4

class CategoryManagementViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: com.woojin.paymanagement.domain.usecase.UpdateCategoryUseCase,
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
                    id = uuid4().toString(),
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

    fun showEditDialog(category: Category) {
        uiState = uiState.copy(
            isEditDialogVisible = true,
            editingCategory = category,
            editCategoryName = category.name,
            editCategoryEmoji = category.emoji
        )
    }

    fun hideEditDialog() {
        uiState = uiState.copy(
            isEditDialogVisible = false,
            editingCategory = null,
            editCategoryName = "",
            editCategoryEmoji = ""
        )
    }

    fun updateEditCategoryName(name: String) {
        uiState = uiState.copy(editCategoryName = name)
    }

    fun updateEditCategoryEmoji(emoji: String) {
        uiState = uiState.copy(editCategoryEmoji = emoji)
    }

    fun updateCategory() {
        val editingCategory = uiState.editingCategory ?: return
        if (uiState.editCategoryName.isBlank() || uiState.editCategoryEmoji.isBlank()) {
            return
        }

        val newCategory = editingCategory.copy(
            name = uiState.editCategoryName.trim(),
            emoji = uiState.editCategoryEmoji.trim()
        )

        // 이름이 변경되었는지 확인
        val nameChanged = editingCategory.name != newCategory.name

        if (nameChanged) {
            // 변경 확인 다이얼로그 표시
            val message = """
                카테고리 이름을 변경하면 해당 카테고리로 저장된 모든 거래 내역의 카테고리 이름도 함께 변경됩니다.

                변경 전: ${editingCategory.name}
                변경 후: ${newCategory.name}

                계속하시겠습니까?
            """.trimIndent()

            uiState = uiState.copy(
                showConfirmDialog = true,
                confirmDialogMessage = message,
                pendingUpdate = { performUpdate(editingCategory, newCategory) }
            )
        } else {
            // 이름이 변경되지 않았으면 바로 업데이트
            performUpdate(editingCategory, newCategory)
        }
    }

    private fun performUpdate(oldCategory: Category, newCategory: Category) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true)
                updateCategoryUseCase(oldCategory, newCategory)
                hideEditDialog()
                hideConfirmDialog()
            } catch (e: IllegalArgumentException) {
                // 이름 중복 에러
                uiState = uiState.copy(error = e.message)
            } catch (e: Exception) {
                uiState = uiState.copy(error = "카테고리 수정 중 오류가 발생했습니다: ${e.message}")
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun showConfirmDialogForUpdate() {
        uiState.pendingUpdate?.invoke()
    }

    fun hideConfirmDialog() {
        uiState = uiState.copy(
            showConfirmDialog = false,
            confirmDialogMessage = "",
            pendingUpdate = null
        )
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}
