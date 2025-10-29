package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.BudgetPlan
import com.woojin.paymanagement.domain.repository.BudgetRepository

class SaveBudgetPlanUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(budgetPlan: BudgetPlan) {
        repository.insertBudgetPlan(budgetPlan)
    }
}
