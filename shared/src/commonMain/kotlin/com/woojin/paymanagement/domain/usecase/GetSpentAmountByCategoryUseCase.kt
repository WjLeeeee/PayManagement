package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.BudgetRepository
import kotlinx.datetime.LocalDate

class GetSpentAmountByCategoryUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(
        categoryName: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Double {
        return repository.getSpentAmountByCategory(categoryName, startDate, endDate)
    }
}
