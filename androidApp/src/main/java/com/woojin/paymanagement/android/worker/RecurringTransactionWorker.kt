package com.woojin.paymanagement.android.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woojin.paymanagement.android.util.RecurringTransactionNotificationHelper
import com.woojin.paymanagement.data.RecurringTransaction
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.usecase.CheckTodayRecurringTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.MarkRecurringTransactionExecutedUseCase
import com.woojin.paymanagement.domain.usecase.SaveTransactionUseCase
import com.woojin.paymanagement.koinInstance
import com.woojin.paymanagement.utils.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.random.Random

/**
 * 매일 오전 9시에 실행되어 오늘 실행할 반복 거래를 확인하는 Worker.
 * - 자동 실행 ON: 반복 거래를 실제로 저장 후 "자동 등록 완료" 알림
 * - 자동 실행 OFF: 기존처럼 "기록해볼까요?" 리마인더 알림
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

            val preferencesManager = PreferencesManager(applicationContext)
            val checkTodayRecurringTransactionsUseCase: CheckTodayRecurringTransactionsUseCase =
                koinInstance!!.get()

            val todayTransactions = checkTodayRecurringTransactionsUseCase().first()

            if (todayTransactions.isEmpty()) {
                return Result.success()
            }

            if (preferencesManager.isRecurringAutoExecuteEnabled()) {
                // 자동 실행 모드: 거래를 실제로 저장
                val saveTransactionUseCase: SaveTransactionUseCase = koinInstance!!.get()
                val markExecutedUseCase: MarkRecurringTransactionExecutedUseCase = koinInstance!!.get()
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

                todayTransactions.forEach { recurringTransaction ->
                    val transaction = recurringTransaction.toTransaction(today)
                    saveTransactionUseCase(transaction)
                    markExecutedUseCase(recurringTransaction.id)
                }

                RecurringTransactionNotificationHelper.sendAutoExecutedNotification(
                    context = applicationContext,
                    transactions = todayTransactions
                )
            } else {
                // 리마인더 모드: 알림만 발송
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

    private fun RecurringTransaction.toTransaction(today: kotlinx.datetime.LocalDate): Transaction {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val random = Random.nextInt(1000, 9999)
        return Transaction(
            id = "${timestamp}_${random}",
            amount = amount,
            type = type,
            category = category,
            merchant = merchant,
            memo = memo,
            date = today,
            incomeType = null,
            paymentMethod = paymentMethod,
            balanceCardId = balanceCardId,
            giftCardId = giftCardId,
            cardName = cardName,
            settlementAmount = null,
            isSettlement = false
        )
    }
}
