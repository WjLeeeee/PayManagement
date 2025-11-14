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
    fun sendRecurringTransactionNotification(context: Context, transactionCount: Int) {
        try {
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

            // 알림 생성
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🔄 반복 거래 알림")
                .setContentText("오늘 실행할 반복 거래가 ${transactionCount}건 있습니다")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("오늘 실행할 반복 거래가 ${transactionCount}건 있습니다.\n탭하여 거래를 등록하세요!")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            Log.d(TAG, "Notification sent for $transactionCount recurring transactions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
        }
    }
}
