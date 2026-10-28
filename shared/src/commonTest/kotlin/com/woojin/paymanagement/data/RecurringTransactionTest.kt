package com.woojin.paymanagement.data

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RecurringTransactionTest {

    private fun monthly(day: Int, handling: WeekendHandling) = RecurringTransaction(
        id = "r", type = TransactionType.EXPENSE, category = "주거", amount = 500_000.0, merchant = "월세",
        paymentMethod = PaymentMethod.CASH, pattern = RecurringPattern.MONTHLY, dayOfMonth = day,
        weekendHandling = handling, createdAt = 0
    )

    /** 2026년 중 실행되는 날짜 목록 */
    private suspend fun RecurringTransaction.runDaysIn2026(): List<LocalDate> {
        val days = mutableListOf<LocalDate>()
        var date = LocalDate(2026, 1, 1)
        while (date.year == 2026) {
            if (shouldExecuteToday(date)) days += date
            date = LocalDate.fromEpochDays(date.toEpochDays() + 1)
        }
        return days
    }

    @Test
    fun day31RunsOnLastDayOfShortMonths() = runTest {
        val days = monthly(31, WeekendHandling.AS_IS).runDaysIn2026()

        assertEquals(12, days.size) // 매달 한 번
        assertEquals(LocalDate(2026, 2, 28), days[1])
        assertEquals(LocalDate(2026, 4, 30), days[3])
    }

    @Test
    fun previousWeekdayCanMoveIntoPreviousMonth() = runTest {
        // 2026-11-01은 일요일 → 10/30(금)에 실행
        val days = monthly(1, WeekendHandling.PREVIOUS_WEEKDAY).runDaysIn2026()

        assertEquals(LocalDate(2026, 10, 30), days.first { it.monthNumber >= 10 && it.dayOfMonth > 1 })
        assertEquals(12, days.size)
    }

    @Test
    fun nextWeekdayCanMoveIntoNextMonth() = runTest {
        // 2026-01-31은 토요일 → 2/2(월), 2/28(토) → 3/2(월)
        val days = monthly(31, WeekendHandling.NEXT_WEEKDAY).runDaysIn2026()

        assertEquals(LocalDate(2026, 2, 2), days[0])
        assertEquals(LocalDate(2026, 3, 2), days[1])
    }
}
