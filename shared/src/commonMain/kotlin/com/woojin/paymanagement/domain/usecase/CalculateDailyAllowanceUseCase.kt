package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.INVESTMENT_INCOME_CATEGORIES
import com.woojin.paymanagement.domain.model.DailyAllowance
import com.woojin.paymanagement.domain.repository.BudgetRepository
import com.woojin.paymanagement.domain.repository.HolidayRepository
import com.woojin.paymanagement.domain.repository.PreferencesRepository
import com.woojin.paymanagement.domain.repository.RecurringTransactionRepository
import com.woojin.paymanagement.utils.PayPeriodCalculator
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * 월급날까지 하루 얼마 계산 (오늘이 속한 급여 기간 기준)
 *
 * 가용액 = 급여 - 기간 내 저축·투자 - 기간 내 지출(오늘 제외) - 내일~급여일 전날 반복지출
 * 하루 한도 = 가용액 ÷ 남은 일수(오늘 포함)
 *
 * 급여는 예산 설정의 고정 급여, 없으면 기간 내 수입 합계. 둘 다 없으면 null
 * 오늘 실행된 반복지출은 오늘 지출로 잡힘 (하루 한도는 하루 동안 그대로)
 */
class CalculateDailyAllowanceUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val payPeriodCalculator: PayPeriodCalculator,
    private val budgetRepository: BudgetRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val holidayRepository: HolidayRepository
) {
    suspend operator fun invoke(transactions: List<Transaction>, today: LocalDate): DailyAllowance? {
        val period = payPeriodCalculator.getCurrentPayPeriod(
            preferencesRepository.getPayday(),
            preferencesRepository.getPaydayAdjustment(),
            currentDate = today
        )
        val periodTransactions = transactions.filter { it.date >= period.startDate && it.date <= period.endDate }

        val monthlySalary = budgetRepository.getBudgetPlanByDate(today).first()?.monthlySalary ?: 0.0
        val income = if (monthlySalary > 0) {
            monthlySalary
        } else {
            periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.displayAmount }
        }
        if (income <= 0) return null

        val saving = periodTransactions
            .filter { it.type == TransactionType.SAVING }
            .sumOf { it.displayAmount }
        // 익절·배당금은 투자에서 돌아온 돈 (급여 기간 요약 카드와 같은 기준)
        val investment = periodTransactions
            .filter { it.type == TransactionType.INVESTMENT }
            .sumOf { if (it.category in INVESTMENT_INCOME_CATEGORIES) -it.displayAmount else it.displayAmount }
        val expenses = periodTransactions.filter { it.type == TransactionType.EXPENSE }
        val todaySpent = expenses.filter { it.date == today }.sumOf { it.displayAmount }
        val otherDaysSpent = expenses.filter { it.date != today }.sumOf { it.displayAmount }

        val upcomingRecurring = getUpcomingRecurringAmount(
            from = today.plus(1, DateTimeUnit.DAY),
            to = period.endDate
        )

        val available = income - saving - investment - otherDaysSpent - upcomingRecurring
        val daysLeft = (period.endDate.toEpochDays() - today.toEpochDays() + 1).toInt()

        return DailyAllowance(
            dailyLimit = available / daysLeft,
            todaySpent = todaySpent,
            daysLeft = daysLeft
        )
    }

    /** from~to 사이에 실행될 반복 지출·저축·투자 합계 */
    private suspend fun getUpcomingRecurringAmount(from: LocalDate, to: LocalDate): Double {
        if (from > to) return 0.0
        val recurring = recurringTransactionRepository.getActiveRecurringTransactions().first()
            .filter { it.type != TransactionType.INCOME }
        if (recurring.isEmpty()) return 0.0

        val holidays = setOf(from.year, to.year)
            .flatMap { holidayRepository.getHolidaysByYear(it) }
            .filter { it.isHoliday }
            .map { it.locdate }
            .toSet()
        val isHoliday: suspend (LocalDate) -> Boolean = { date ->
            "${date.year}${date.monthNumber.toString().padStart(2, '0')}${date.dayOfMonth.toString().padStart(2, '0')}" in holidays
        }

        var total = 0.0
        var date = from
        while (date <= to) {
            recurring.forEach { if (it.shouldExecuteToday(date, isHoliday)) total += it.amount }
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return total
    }
}
