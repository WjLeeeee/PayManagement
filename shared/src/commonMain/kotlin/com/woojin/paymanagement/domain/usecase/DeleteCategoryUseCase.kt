package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.CategoryRepository

class DeleteCategoryUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(categoryId: String) {
        repository.deleteCategory(categoryId)
    }
}
