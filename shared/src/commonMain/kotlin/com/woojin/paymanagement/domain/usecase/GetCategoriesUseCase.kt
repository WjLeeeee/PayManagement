package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(
    private val repository: CategoryRepository
) {
    operator fun invoke(type: TransactionType): Flow<List<Category>> {
        return repository.getCategoriesByType(type)
    }
}
