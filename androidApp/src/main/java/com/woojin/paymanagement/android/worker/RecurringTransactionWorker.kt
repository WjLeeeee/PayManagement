package com.woojin.paymanagement.android.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woojin.paymanagement.android.util.RecurringTransactionNotificationHelper
import com.woojin.paymanagement.domain.usecase.CheckTodayRecurringTransactionsUseCase
import com.woojin.paymanagement.koinInstance
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * 매일 오전 9시에 실행되어 오늘 실행할 반복 거래를 확인하고 알림을 보내는 Worker
 */
class RecurringTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (koinInstance == null) {
                return Result.failure()
            }

            val checkTodayRecurringTransactionsUseCase: CheckTodayRecurringTransactionsUseCase =
                koinInstance!!.get()

            val todayTransactions = checkTodayRecurringTransactionsUseCase().first()

            if (todayTransactions.isNotEmpty()) {
                RecurringTransactionNotificationHelper.sendRecurringTransactionNotification(
                    context = applicationContext,
                    transactions = todayTransactions
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
