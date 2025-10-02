package com.woojin.paymanagement.data

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * 알림에서 파싱된 거래 내역을 임시 저장하는 Entity
 */
@Serializable
data class ParsedTransaction(
    val id: String,
    val amount: Double,          // 승인금액
    val merchantName: String,    // 가맹점명
    val date: LocalDate,         // 승인일시 (년도 + 월/일)
    val rawNotification: String, // 원본 알림 텍스트 (디버깅용)
    val isProcessed: Boolean = false, // 거래 추가 화면으로 넘어갔는지 여부
    val createdAt: Long = Clock.System.now().toEpochMilliseconds() // 알림 수신 시간
)