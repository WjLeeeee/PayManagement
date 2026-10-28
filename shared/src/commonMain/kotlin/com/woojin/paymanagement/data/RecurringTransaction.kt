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
    WEEKLY,   // 매주 (특정 요일)
    DAILY     // 매일
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
    val cardName: String? = null,  // 커스텀 카드명 (CARD 결제 시)
    val pattern: RecurringPattern,
    val dayOfMonth: Int? = null,  // 1~31 (MONTHLY일 때 사용)
    val dayOfWeek: Int? = null,   // 1~7 (월~일, WEEKLY일 때 사용)
    val weekendHandling: WeekendHandling = WeekendHandling.AS_IS,  // 주말 처리 방식
    val includeWeekends: Boolean = true,  // 주말 포함 여부 (DAILY일 때 사용)
    val isActive: Boolean = true,  // 활성화 여부
    val createdAt: Long,
    val lastExecutedDate: String? = null  // "2025-01-15" 마지막으로 자동 추가된 날짜
) {
    /**
     * 오늘 실행해야 하는지 확인 (주말/공휴일 처리 포함)
     */
    suspend fun shouldExecuteToday(
        today: LocalDate,
        isHoliday: suspend (LocalDate) -> Boolean = { false }
    ): Boolean {
        if (!isActive) return false

        return when (pattern) {
            RecurringPattern.MONTHLY -> {
                val day = dayOfMonth ?: return false
                // 지난달·이번 달·다음 달 실행일 중 (주말·공휴일 조정 후) 오늘인 게 있으면 실행
                // 조정으로 달을 넘는 경우(예: 11/1(일) → 10/30(금))도 잡으려고 앞뒤 달까지 봄
                val thisMonth = LocalDate(today.year, today.month, 1)
                (-1..1).any { offset ->
                    adjustForWeekend(monthlyDate(thisMonth.plus(DatePeriod(months = offset)), day), isHoliday) == today
                }
            }
            RecurringPattern.WEEKLY -> {
                // 매주 패턴은 특정 요일이므로 주말 처리 필요 없음
                // DayOfWeek.ordinal + 1 = ISO day number (1=월요일, 7=일요일)
                dayOfWeek != null && (today.dayOfWeek.ordinal + 1) == dayOfWeek
            }
            RecurringPattern.DAILY -> {
                // 주말 미포함 설정이면 토/일에는 실행하지 않음
                includeWeekends || (today.dayOfWeek != DayOfWeek.SATURDAY && today.dayOfWeek != DayOfWeek.SUNDAY)
            }
        }
    }

    /** 그 달의 실행일. 그 달에 없는 날짜(예: 4월 31일)는 그 달 마지막 날 (급여일 계산과 같은 규칙) */
    private fun monthlyDate(firstOfMonth: LocalDate, day: Int): LocalDate {
        val lastDay = firstOfMonth.plus(DatePeriod(months = 1)).plus(DatePeriod(days = -1)).dayOfMonth
        return LocalDate(firstOfMonth.year, firstOfMonth.month, minOf(day, lastDay))
    }

    /**
     * 주말/공휴일이면 weekendHandling 설정에 따라 날짜 조정
     */
    private suspend fun adjustForWeekend(
        date: LocalDate,
        isHoliday: suspend (LocalDate) -> Boolean
    ): LocalDate {
        // 주말/공휴일이 아니면 그대로 반환
        if (!isNonBusinessDay(date, isHoliday)) {
            return date
        }

        return when (weekendHandling) {
            WeekendHandling.AS_IS -> date
            WeekendHandling.PREVIOUS_WEEKDAY -> {
                // 이전 평일 찾기
                var adjusted = date
                while (isNonBusinessDay(adjusted, isHoliday)) {
                    adjusted = adjusted.plus(DatePeriod(days = -1))
                }
                adjusted
            }
            WeekendHandling.NEXT_WEEKDAY -> {
                // 다음 평일 찾기
                var adjusted = date
                while (isNonBusinessDay(adjusted, isHoliday)) {
                    adjusted = adjusted.plus(DatePeriod(days = 1))
                }
                adjusted
            }
        }
    }

    private suspend fun isNonBusinessDay(
        date: LocalDate,
        isHoliday: suspend (LocalDate) -> Boolean
    ): Boolean {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        return isWeekend || isHoliday(date)
    }

    /**
     * 이미 오늘 실행됐는지 확인
     */
    fun isExecutedToday(today: LocalDate): Boolean {
        return lastExecutedDate == today.toString()
    }
}
