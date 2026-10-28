package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.BudgetPlan
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.RecurringPattern
import com.woojin.paymanagement.data.RecurringTransaction
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.model.Holiday
import com.woojin.paymanagement.domain.repository.BudgetRepository
import com.woojin.paymanagement.domain.repository.HolidayRepository
import com.woojin.paymanagement.domain.repository.PreferencesRepository
import com.woojin.paymanagement.domain.repository.RecurringTransactionRepository
import com.woojin.paymanagement.utils.PayPeriodCalculator
import com.woojin.paymanagement.utils.PaydayAdjustment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 급여일 25일 → 급여 기간 2025-06-25(수) ~ 2025-07-24(목)
 */
class CalculateDailyAllowanceUseCaseTest {

    @Test
    fun salaryMinusSavingInvestmentPastSpendingAndUpcomingRecurring() = runTest {
        val today = LocalDate(2025, 7, 15)
        val useCase = useCase(
            salary = 3_000_000.0,
            recurring = listOf(
                recurring(TransactionType.EXPENSE, 150_000.0, dayOfMonth = 20), // 7/20(일) 그대로 → 남은 기간에 포함
                recurring(TransactionType.EXPENSE, 77_777.0, dayOfMonth = 15),  // 오늘 → 오늘 지출로 잡히므로 제외
                recurring(TransactionType.INCOME, 1_000_000.0, dayOfMonth = 18) // 수입 → 제외
            )
        )
        val transactions = listOf(
            tx(TransactionType.INCOME, 3_500_000.0, LocalDate(2025, 6, 25)),     // 고정 급여가 있으면 무시
            tx(TransactionType.SAVING, 500_000.0, LocalDate(2025, 6, 25)),
            tx(TransactionType.INVESTMENT, 200_000.0, LocalDate(2025, 7, 1)),
            tx(TransactionType.INVESTMENT, 50_000.0, LocalDate(2025, 7, 10), category = "익절"), // 돌아온 돈
            tx(TransactionType.EXPENSE, 1_000_000.0, LocalDate(2025, 7, 5)),
            tx(TransactionType.EXPENSE, 40_000.0, LocalDate(2025, 7, 14), settlement = 30_000.0), // 실부담 1만
            tx(TransactionType.EXPENSE, 20_000.0, today),
            tx(TransactionType.EXPENSE, 999_999.0, LocalDate(2025, 6, 20))       // 이전 기간
        )

        val result = useCase(transactions, today)!!

        // 3,000,000 - 500,000 - 150,000 - 1,010,000 - 150,000 = 1,190,000 ÷ 10일
        assertEquals(10, result.daysLeft)
        assertEquals(119_000.0, result.dailyLimit)
        assertEquals(20_000.0, result.todaySpent)
        assertEquals(99_000.0, result.remainingToday)
    }

    @Test
    fun withoutSalaryUsesPeriodIncomeAndLastDayHasNoUpcomingRecurring() = runTest {
        val today = LocalDate(2025, 7, 24)
        val useCase = useCase(
            salary = null,
            recurring = listOf(recurring(TransactionType.EXPENSE, 150_000.0, dayOfMonth = 20))
        )
        val transactions = listOf(
            tx(TransactionType.INCOME, 2_000_000.0, LocalDate(2025, 6, 25)),
            tx(TransactionType.INCOME, 9_999_999.0, LocalDate(2025, 5, 30)) // 이전 기간
        )

        val result = useCase(transactions, today)!!

        assertEquals(1, result.daysLeft)
        assertEquals(2_000_000.0, result.dailyLimit)
    }

    @Test
    fun noSalaryAndNoIncomeReturnsNull() = runTest {
        val today = LocalDate(2025, 7, 15)
        val transactions = listOf(tx(TransactionType.EXPENSE, 10_000.0, today))

        assertNull(useCase(salary = null)(transactions, today))
    }

    private fun useCase(salary: Double?, recurring: List<RecurringTransaction> = emptyList()) =
        CalculateDailyAllowanceUseCase(
            preferencesRepository = FakePreferences,
            payPeriodCalculator = PayPeriodCalculator(),
            budgetRepository = FakeBudget(
                salary?.let { BudgetPlan("plan", LocalDate(2025, 1, 1), it, LocalDate(2025, 1, 1)) }
            ),
            recurringTransactionRepository = FakeRecurring(recurring),
            holidayRepository = NoHolidays
        )

    private fun tx(
        type: TransactionType,
        amount: Double,
        date: LocalDate,
        category: String = "기타",
        settlement: Double? = null
    ) = Transaction(
        id = "$type-$date-$amount",
        amount = amount,
        type = type,
        category = category,
        memo = "",
        date = date,
        settlementAmount = settlement,
        isSettlement = settlement != null
    )

    private fun recurring(type: TransactionType, amount: Double, dayOfMonth: Int) = RecurringTransaction(
        id = "$type-$dayOfMonth",
        type = type,
        category = "기타",
        amount = amount,
        merchant = "test",
        paymentMethod = PaymentMethod.CASH,
        pattern = RecurringPattern.MONTHLY,
        dayOfMonth = dayOfMonth,
        createdAt = 0
    )

    private object FakePreferences : PreferencesRepository {
        override fun getPayday() = 25
        override fun getPaydayAdjustment() = PaydayAdjustment.BEFORE_WEEKEND
        override fun setPayday(day: Int) = TODO()
        override fun setPaydayAdjustment(adjustment: PaydayAdjustment) = TODO()
        override fun isMoneyVisible() = TODO()
        override fun setMoneyVisible(visible: Boolean) = TODO()
        override fun getMonthlySalary() = TODO()
        override fun setMonthlySalary(salary: Double) = TODO()
        override fun isSharedMode() = TODO()
        override fun setIsSharedMode(enabled: Boolean) = TODO()
        override fun getHolidaysRefreshedAt() = TODO()
        override fun setHolidaysRefreshedAt(time: Long) = TODO()
    }

    private class FakeBudget(private val plan: BudgetPlan?) : BudgetRepository {
        override fun getBudgetPlanByDate(date: LocalDate): Flow<BudgetPlan?> = flowOf(plan)
        override fun getAllBudgetPlans() = TODO()
        override suspend fun insertBudgetPlan(budgetPlan: BudgetPlan) = TODO()
        override suspend fun deleteBudgetPlan(id: String) = TODO()
        override suspend fun updateBudgetPlanTransfers(id: String, transfers: List<com.woojin.paymanagement.data.TransferItem>) = TODO()
        override fun getTransferChecksByPeriod(periodStartDate: LocalDate) = TODO()
        override suspend fun setTransferChecked(periodStartDate: LocalDate, transferItemId: String, checked: Boolean) = TODO()
        override fun getCategoryBudgetsByPlanId(budgetPlanId: String) = TODO()
        override suspend fun insertCategoryBudget(categoryBudget: com.woojin.paymanagement.data.CategoryBudget) = TODO()
        override suspend fun updateCategoryBudget(
            id: String,
            allocatedAmount: Double,
            memo: String?,
            items: List<com.woojin.paymanagement.data.BudgetItem>,
            accountAllocations: List<com.woojin.paymanagement.data.BudgetAccountAllocation>
        ) = TODO()
        override suspend fun updateCategoryBudgetFull(
            id: String,
            categoryIds: List<String>,
            categoryName: String,
            categoryEmoji: String,
            allocatedAmount: Double,
            memo: String?,
            items: List<com.woojin.paymanagement.data.BudgetItem>,
            accountAllocations: List<com.woojin.paymanagement.data.BudgetAccountAllocation>
        ) = TODO()
        override suspend fun deleteCategoryBudget(id: String) = TODO()
        override suspend fun getSpentAmountByCategory(categoryName: String, startDate: LocalDate, endDate: LocalDate) = TODO()
    }

    private class FakeRecurring(private val items: List<RecurringTransaction>) : RecurringTransactionRepository {
        override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(items)
        override fun getAllRecurringTransactions() = TODO()
        override suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction) = TODO()
        override suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction) = TODO()
        override suspend fun updateLastExecutedDate(id: String, date: String) = TODO()
        override suspend fun deleteRecurringTransaction(id: String) = TODO()
    }

    private object NoHolidays : HolidayRepository {
        override suspend fun getHolidaysByYear(year: Int): List<Holiday> = emptyList()
        override suspend fun getHolidayByDate(date: String): Holiday? = null
        override suspend fun fetchAndSaveHolidays(serviceKey: String, years: List<Int>) = TODO()
        override suspend fun deleteHolidaysByYear(year: Int) = TODO()
        override suspend fun getLatestHolidayDate() = TODO()
    }
}
