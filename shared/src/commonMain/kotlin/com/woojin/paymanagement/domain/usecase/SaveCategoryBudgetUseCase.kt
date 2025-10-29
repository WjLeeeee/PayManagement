package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.CategoryBudget
import com.woojin.paymanagement.domain.repository.BudgetRepository

class SaveCategoryBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(categoryBudget: CategoryBudget) {
        repository.insertCategoryBudget(categoryBudget)
    }
}
