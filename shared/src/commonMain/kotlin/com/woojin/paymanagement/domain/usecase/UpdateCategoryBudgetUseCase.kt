package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.BudgetRepository

class UpdateCategoryBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(id: String, allocatedAmount: Double, memo: String? = null) {
        repository.updateCategoryBudget(id, allocatedAmount, memo)
    }

    suspend operator fun invoke(
        id: String,
        allocatedAmount: Double,
        memo: String?,
        categoryIds: List<String>,
        categoryName: String,
        categoryEmoji: String
    ) {
        repository.updateCategoryBudgetFull(id, categoryIds, categoryName, categoryEmoji, allocatedAmount, memo)
    }
}
