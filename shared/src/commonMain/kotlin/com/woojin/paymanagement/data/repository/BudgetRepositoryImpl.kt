package com.woojin.paymanagement.data.repository

import com.woojin.paymanagement.data.BudgetPlan
import com.woojin.paymanagement.data.CategoryBudget
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class BudgetRepositoryImpl(
    private val databaseHelper: DatabaseHelper
) : BudgetRepository {

    override fun getBudgetPlanByPeriod(startDate: LocalDate, endDate: LocalDate): Flow<BudgetPlan?> {
        return databaseHelper.getBudgetPlanByPeriod(startDate, endDate)
    }

    override fun getAllBudgetPlans(): Flow<List<BudgetPlan>> {
        return databaseHelper.getAllBudgetPlans()
    }

    override suspend fun insertBudgetPlan(budgetPlan: BudgetPlan) {
        databaseHelper.insertBudgetPlan(budgetPlan)
    }

    override suspend fun deleteBudgetPlan(id: String) {
        databaseHelper.deleteBudgetPlan(id)
    }

    override fun getCategoryBudgetsByPlanId(budgetPlanId: String): Flow<List<CategoryBudget>> {
        return databaseHelper.getCategoryBudgetsByPlanId(budgetPlanId)
    }

    override suspend fun insertCategoryBudget(categoryBudget: CategoryBudget) {
        databaseHelper.insertCategoryBudget(categoryBudget)
    }

    override suspend fun updateCategoryBudget(id: String, allocatedAmount: Double) {
        databaseHelper.updateCategoryBudget(id, allocatedAmount)
    }

    override suspend fun deleteCategoryBudget(id: String) {
        databaseHelper.deleteCategoryBudget(id)
    }

    override suspend fun getSpentAmountByCategory(
        categoryName: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Double {
        return databaseHelper.getSpentAmountByCategory(categoryName, startDate, endDate)
    }
}
