package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.BudgetAccountAllocation
import com.woojin.paymanagement.data.BudgetItem
import com.woojin.paymanagement.domain.repository.BudgetRepository

class UpdateCategoryBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(
        id: String,
        allocatedAmount: Double,
        memo: String? = null,
        items: List<BudgetItem> = emptyList(),
        accountAllocations: List<BudgetAccountAllocation> = emptyList()
    ) {
        repository.updateCategoryBudget(id, allocatedAmount, memo, items, accountAllocations)
    }

    suspend operator fun invoke(
        id: String,
        allocatedAmount: Double,
        memo: String?,
        categoryIds: List<String>,
        categoryName: String,
        categoryEmoji: String,
        items: List<BudgetItem> = emptyList(),
        accountAllocations: List<BudgetAccountAllocation> = emptyList()
    ) {
        repository.updateCategoryBudgetFull(id, categoryIds, categoryName, categoryEmoji, allocatedAmount, memo, items, accountAllocations)
    }
}
