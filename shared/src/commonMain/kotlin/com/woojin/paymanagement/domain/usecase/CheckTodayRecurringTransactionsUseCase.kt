package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.RecurringTransaction
import com.woojin.paymanagement.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * 오늘 실행해야 하는 반복 거래 목록을 반환하는 UseCase
 */
class CheckTodayRecurringTransactionsUseCase(
    private val repository: RecurringTransactionRepository
) {
    operator fun invoke(): Flow<List<RecurringTransaction>> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        return repository.getActiveRecurringTransactions().map { transactions ->
            transactions.filter { transaction ->
                transaction.shouldExecuteToday(today) && !transaction.isExecutedToday(today)
            }
        }
    }
}
