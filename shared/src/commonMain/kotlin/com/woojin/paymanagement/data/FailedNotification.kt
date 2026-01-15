package com.woojin.paymanagement.data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * 파싱에 실패한 알림을 저장하는 Entity
 * 나중에 패턴 분석 및 파싱 로직 개선에 활용
 */
@Serializable
data class FailedNotification(
    val id: Long = 0,
    val packageName: String,     // 알림을 보낸 앱 패키지명 (예: com.shcard.smartpay)
    val title: String,            // 알림 제목
    val text: String,             // 알림 내용
    val bigText: String? = null,  // 확장된 알림 내용
    val failureReason: String? = null, // 실패 원인 (예: "금액 파싱 실패", "날짜 파싱 실패")
    val createdAt: Long = Clock.System.now().toEpochMilliseconds() // 알림 수신 시간
)
