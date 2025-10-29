package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.BudgetRepository

class UpdateCategoryBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(id: String, allocatedAmount: Double) {
        repository.updateCategoryBudget(id, allocatedAmount)
    }
}
