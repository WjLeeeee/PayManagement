package com.woojin.paymanagement.android.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.woojin.paymanagement.android.MainActivity

object RecurringTransactionNotificationHelper {
    private const val TAG = "RecurringTransactionNotif"
    private const val NOTIFICATION_CHANNEL_ID = "recurring_transaction_channel"
    private const val NOTIFICATION_CHANNEL_NAME = "반복 거래 알림"
    private const val NOTIFICATION_ID = 2001

    fun initialize(context: Context) {
        createNotificationChannel(context)
    }

    /**
     * 알림 채널 생성 (Android 8.0 이상에서 필수)
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "반복 거래 실행 알림"
                enableVibration(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 반복 거래 알림 전송
     */
    fun sendRecurringTransactionNotification(
        context: Context,
        transactions: List<com.woojin.paymanagement.data.RecurringTransaction>
    ) {
        try {
            if (transactions.isEmpty()) return

            // 알림 클릭 시 반복 거래 관리 화면으로 이동하는 Intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(MainActivity.EXTRA_NAVIGATE_TO_RECURRING_TRANSACTIONS, true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // 첫 번째 거래 정보로 알림 내용 구성
            val firstTransaction = transactions.first()
            val isIncome = firstTransaction.type == com.woojin.paymanagement.data.TransactionType.INCOME

            // 패턴 텍스트 (매달 / 매주)
            val patternText = when (firstTransaction.pattern) {
                com.woojin.paymanagement.data.RecurringPattern.MONTHLY -> "매달"
                com.woojin.paymanagement.data.RecurringPattern.WEEKLY -> "매주"
            }

            // 금액 포맷 (콤마 추가)
            val amount = firstTransaction.amount.toInt()
            val formattedAmount = String.format("%,d", amount)

            // 제목
            val title = if (isIncome) {
                "📅 오늘은 정기수입 데이!"
            } else {
                "📅 오늘은 정기지출 데이!"
            }

            // 내용
            val contentText = if (transactions.size == 1) {
                // 1건일 때
                "$patternText 오는 ${firstTransaction.category} ${formattedAmount}원~ 오늘도 기록해볼까요?"
            } else {
                // 여러 건일 때
                "$patternText 오는 ${firstTransaction.category} ${formattedAmount}원 외 ${transactions.size - 1}건~ 오늘도 기록해볼까요?"
            }

            // 알림 생성
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(contentText)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            Log.d(TAG, "Notification sent for ${transactions.size} recurring transactions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
        }
    }
}
