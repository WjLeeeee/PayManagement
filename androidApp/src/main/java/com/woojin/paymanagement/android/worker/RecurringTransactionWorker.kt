package com.woojin.paymanagement.android.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woojin.paymanagement.android.util.RecurringTransactionNotificationHelper
import com.woojin.paymanagement.domain.usecase.CheckTodayRecurringTransactionsUseCase
import com.woojin.paymanagement.koinInstance
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
            // Koin에서 UseCase 가져오기
            val checkTodayRecurringTransactionsUseCase: CheckTodayRecurringTransactionsUseCase =
                requireNotNull(koinInstance).get()

            // 오늘 실행할 반복 거래 확인 (Flow를 collect)
            var todayTransactions = emptyList<com.woojin.paymanagement.data.RecurringTransaction>()
            checkTodayRecurringTransactionsUseCase().collect { transactions ->
                todayTransactions = transactions
            }

            // 오늘 실행할 항목이 있으면 알림 전송
            if (todayTransactions.isNotEmpty()) {
                RecurringTransactionNotificationHelper.sendRecurringTransactionNotification(
                    context = applicationContext,
                    transactionCount = todayTransactions.size
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
