package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.data.BudgetPlan
import com.woojin.paymanagement.data.CategoryBudget
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface BudgetRepository {
    fun getBudgetPlanByPeriod(startDate: LocalDate, endDate: LocalDate): Flow<BudgetPlan?>
    fun getAllBudgetPlans(): Flow<List<BudgetPlan>>
    suspend fun insertBudgetPlan(budgetPlan: BudgetPlan)
    suspend fun deleteBudgetPlan(id: String)

    fun getCategoryBudgetsByPlanId(budgetPlanId: String): Flow<List<CategoryBudget>>
    suspend fun insertCategoryBudget(categoryBudget: CategoryBudget)
    suspend fun updateCategoryBudget(id: String, allocatedAmount: Double)
    suspend fun deleteCategoryBudget(id: String)

    suspend fun getSpentAmountByCategory(categoryName: String, startDate: LocalDate, endDate: LocalDate): Double
}
