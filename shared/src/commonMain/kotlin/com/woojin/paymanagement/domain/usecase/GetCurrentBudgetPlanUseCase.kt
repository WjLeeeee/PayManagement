package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.BudgetPlan
import com.woojin.paymanagement.domain.repository.BudgetRepository
import com.woojin.paymanagement.utils.PayPeriod
import kotlinx.coroutines.flow.Flow

class GetCurrentBudgetPlanUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(payPeriod: PayPeriod): Flow<BudgetPlan?> {
        return repository.getBudgetPlanByPeriod(
            startDate = payPeriod.startDate,
            endDate = payPeriod.endDate
        )
    }
}
