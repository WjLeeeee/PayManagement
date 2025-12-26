package com.woojin.paymanagement.utils

import android.content.Context
import android.content.Intent

/**
 * Android 이메일 전송 헬퍼
 */
actual class EmailHelper(
    private val context: Context
) {
    actual fun sendSupportEmail(
        email: String,
        subject: String,
        appVersion: String,
        osVersion: String,
        deviceModel: String
    ) {
        val builder = StringBuilder()
        builder.append("\n\n")
        builder.append("안드로이드 앱버전 : $appVersion")
        builder.append("\n")
        builder.append("안드로이드 OS버전 : $osVersion($deviceModel)")
        builder.append("\n")
        builder.append("오류 및 건의사항 편하게 문의주세요 :)")
        builder.append("\n")

        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, builder.toString())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            type = "text/html"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            // Gmail 앱으로 시도
            intent.setPackage("com.google.android.gm")
            context.startActivity(intent)
        } catch (e: Exception) {
            // Gmail이 없으면 이메일 선택기 표시
            e.printStackTrace()
            try {
                intent.setPackage(null)
                context.startActivity(Intent.createChooser(intent, "이메일 전송").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
