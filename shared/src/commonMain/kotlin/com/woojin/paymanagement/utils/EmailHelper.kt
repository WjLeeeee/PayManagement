package com.woojin.paymanagement.utils

/**
 * 이메일 전송 헬퍼
 */
expect class EmailHelper {
    fun sendSupportEmail(
        email: String,
        subject: String,
        appVersion: String,
        osVersion: String,
        deviceModel: String
    )
}
