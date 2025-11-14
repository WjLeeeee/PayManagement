package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.RecurringTransactionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * 반복 거래를 오늘 실행했다고 표시하는 UseCase
 */
class MarkRecurringTransactionExecutedUseCase(
    private val repository: RecurringTransactionRepository
) {
    suspend operator fun invoke(recurringTransactionId: String) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        repository.updateLastExecutedDate(recurringTransactionId, today.toString())
    }
}
