package com.woojin.paymanagement.data

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * 반복 거래 패턴
 */
enum class RecurringPattern {
    MONTHLY,  // 매달 (특정 날짜)
    WEEKLY    // 매주 (특정 요일)
}

/**
 * 주말 처리 방식
 */
enum class WeekendHandling {
    AS_IS,           // 그대로 적용
    PREVIOUS_WEEKDAY, // 이전 평일로
    NEXT_WEEKDAY      // 다음 평일로
}

/**
 * 반복 거래
 */
data class RecurringTransaction(
    val id: String,
    val type: TransactionType,
    val category: String,
    val amount: Double,
    val merchant: String,  // 사용처 (필수)
    val memo: String = "",  // 메모 (선택)
    val paymentMethod: PaymentMethod,
    val balanceCardId: String? = null,
    val giftCardId: String? = null,
    val pattern: RecurringPattern,
    val dayOfMonth: Int? = null,  // 1~31 (MONTHLY일 때 사용)
    val dayOfWeek: Int? = null,   // 1~7 (월~일, WEEKLY일 때 사용)
    val weekendHandling: WeekendHandling = WeekendHandling.AS_IS,  // 주말 처리 방식
    val isActive: Boolean = true,  // 활성화 여부
    val createdAt: Long,
    val lastExecutedDate: String? = null  // "2025-01-15" 마지막으로 자동 추가된 날짜
) {
    /**
     * 오늘 실행해야 하는지 확인 (주말 처리 포함)
     */
    fun shouldExecuteToday(today: LocalDate): Boolean {
        if (!isActive) return false

        // 원래 실행 날짜 계산
        val originalDate = when (pattern) {
            RecurringPattern.MONTHLY -> {
                if (dayOfMonth == null) return false
                try {
                    LocalDate(today.year, today.month, dayOfMonth)
                } catch (e: Exception) {
                    // 해당 월에 없는 날짜 (예: 2월 30일)
                    return false
                }
            }
            RecurringPattern.WEEKLY -> {
                // 매주 패턴은 특정 요일이므로 주말 처리 필요 없음
                // DayOfWeek.ordinal + 1 = ISO day number (1=월요일, 7=일요일)
                return dayOfWeek != null && (today.dayOfWeek.ordinal + 1) == dayOfWeek
            }
        }

        // 주말 처리 적용
        val adjustedDate = adjustForWeekend(originalDate)
        return today == adjustedDate
    }

    /**
     * 주말이면 weekendHandling 설정에 따라 날짜 조정
     */
    private fun adjustForWeekend(date: LocalDate): LocalDate {
        // 주말이 아니면 그대로 반환
        if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
            return date
        }

        return when (weekendHandling) {
            WeekendHandling.AS_IS -> date
            WeekendHandling.PREVIOUS_WEEKDAY -> {
                // 이전 평일 찾기
                var adjusted = date
                while (adjusted.dayOfWeek == DayOfWeek.SATURDAY || adjusted.dayOfWeek == DayOfWeek.SUNDAY) {
                    adjusted = adjusted.plus(DatePeriod(days = -1))
                }
                adjusted
            }
            WeekendHandling.NEXT_WEEKDAY -> {
                // 다음 평일 찾기
                var adjusted = date
                while (adjusted.dayOfWeek == DayOfWeek.SATURDAY || adjusted.dayOfWeek == DayOfWeek.SUNDAY) {
                    adjusted = adjusted.plus(DatePeriod(days = 1))
                }
                adjusted
            }
        }
    }

    /**
     * 이미 오늘 실행됐는지 확인
     */
    fun isExecutedToday(today: LocalDate): Boolean {
        return lastExecutedDate == today.toString()
    }
}
