package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.BudgetRepository

class DeleteBudgetPlanUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteBudgetPlan(id)
    }
}
