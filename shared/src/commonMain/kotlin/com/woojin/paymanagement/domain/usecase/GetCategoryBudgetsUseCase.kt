package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.CategoryBudget
import com.woojin.paymanagement.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class GetCategoryBudgetsUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(budgetPlanId: String): Flow<List<CategoryBudget>> {
        return repository.getCategoryBudgetsByPlanId(budgetPlanId)
    }
}
