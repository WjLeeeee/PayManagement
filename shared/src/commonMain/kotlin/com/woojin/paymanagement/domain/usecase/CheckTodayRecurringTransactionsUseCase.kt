package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.RecurringTransaction
import com.woojin.paymanagement.domain.repository.HolidayRepository
import com.woojin.paymanagement.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * 오늘 실행해야 하는 반복 거래 목록을 반환하는 UseCase
 */
class CheckTodayRecurringTransactionsUseCase(
    private val repository: RecurringTransactionRepository,
    private val holidayRepository: HolidayRepository
) {
    operator fun invoke(): Flow<List<RecurringTransaction>> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        return repository.getActiveRecurringTransactions().map { transactions ->
            transactions.filter { transaction ->
                transaction.shouldExecuteToday(today) { date -> isHoliday(date) } && !transaction.isExecutedToday(today)
            }
        }
    }

    private suspend fun isHoliday(date: LocalDate): Boolean {
        // YYYYMMDD 형식으로 변환
        val dateStr = "${date.year}${date.monthNumber.toString().padStart(2, '0')}${date.dayOfMonth.toString().padStart(2, '0')}"
        return holidayRepository.getHolidayByDate(dateStr)?.isHoliday == true
    }
}
