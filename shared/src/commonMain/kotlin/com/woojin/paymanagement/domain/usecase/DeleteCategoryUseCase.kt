package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.CategoryRepository

class DeleteCategoryUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(categoryId: String) {
        // 소분류 먼저 삭제 후 상위 카테고리 삭제
        repository.deleteSubCategoriesByParentId(categoryId)
        repository.deleteCategory(categoryId)
    }
}
