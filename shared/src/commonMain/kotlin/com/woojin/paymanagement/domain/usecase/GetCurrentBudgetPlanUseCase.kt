package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.BudgetPlan
import com.woojin.paymanagement.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetCurrentBudgetPlanUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(date: LocalDate): Flow<BudgetPlan?> {
        return repository.getBudgetPlanByDate(date)
    }
}
