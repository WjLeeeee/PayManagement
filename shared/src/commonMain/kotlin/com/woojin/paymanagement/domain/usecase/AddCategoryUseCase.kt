package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first

class AddCategoryUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category) {
        val existingCategories = repository.getCategoriesByType(category.type).first()
        val hasDuplicate = if (category.parentId != null) {
            existingCategories.any { it.parentId == category.parentId && it.name == category.name }
        } else {
            existingCategories.any { it.parentId == null && it.name == category.name }
        }

        if (hasDuplicate) {
            throw IllegalArgumentException("이미 동일한 이름의 카테고리가 존재합니다: ${category.name}")
        }

        repository.insertCategory(category)
    }
}
