package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.domain.repository.CategoryRepository

class AddCategoryUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category) {
        repository.insertCategory(category)
    }
}
