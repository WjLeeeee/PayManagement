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
import com.woojin.paymanagement.data.ParsedTransaction

object TransactionNotificationHelper {
    private const val TAG = "TransactionNotification"
    private const val NOTIFICATION_CHANNEL_ID = "card_transaction_channel"
    private const val NOTIFICATION_CHANNEL_NAME = "카드 결제 알림"
    private var notificationId = 1001

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
                description = "카드 결제 내역 파싱 알림"
                enableVibration(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 거래 알림 전송
     */
    fun sendTransactionNotification(context: Context, transaction: ParsedTransaction) {
        try {
            // 알림 클릭 시 앱 실행 Intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // 금액 포맷팅
            val formattedAmount = String.format("%,d원", transaction.amount.toInt())

            // 알림 생성
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("💳 카드 결제 내역")
                .setContentText("${transaction.merchantName} - $formattedAmount")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${transaction.merchantName}에서 $formattedAmount 결제되었습니다.\n등록해보세요!")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId++, notification)

            Log.d(TAG, "Notification sent for transaction: ${transaction.merchantName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
        }
    }
}