package com.woojin.paymanagement.utils

import com.woojin.paymanagement.domain.repository.HolidayRepository
import kotlinx.datetime.*

object Utils {
    fun formatAmount(amount: Double): String {
        val intAmount = kotlin.math.abs(amount.toInt())
        return intAmount.toString().reversed().chunked(3).joinToString(",").reversed()
    }
}

data class PayPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val displayText: String
)

class PayPeriodCalculator(
    private val holidayRepository: HolidayRepository? = null
) {

    /**
     * 날짜가 속한 급여 기간 = (그 날짜 이전 가장 가까운 실제 급여일) ~ (다음 실제 급여일 전날).
     *
     * 실제 급여일은 주말·공휴일 조정으로 이웃 달로 넘어갈 수 있음
     * (예: 급여일 1일 + 이전 평일 → 11/1(일)이면 10/30, 급여일 31일 + 다음 평일 → 1/31(토)이면 2/2).
     * 그래서 "이번 달/지난 달 급여일"만 비교하지 않고 앞뒤 두 달씩의 실제 급여일 중에서 고름.
     * 조정이 달을 넘지 않는 일반적인 경우엔 예전 계산과 결과가 같음.
     */
    suspend fun getCurrentPayPeriod(
        payday: Int,
        adjustment: PaydayAdjustment,
        currentDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): PayPeriod {
        val paydays = (-2..2).map { offset ->
            val monthDate = currentDate.plus(offset, DateTimeUnit.MONTH)
            calculateActualPayday(monthDate.year, monthDate.month, payday, adjustment)
        }.distinct().sorted()

        val startDate = paydays.last { it <= currentDate }
        val nextPayday = paydays.first { it > startDate }
        val endDate = nextPayday.minus(1, DateTimeUnit.DAY)

        return PayPeriod(
            startDate = startDate,
            endDate = endDate,
            displayText = formatPayPeriodDisplay(startDate, endDate)
        )
    }

    /** 다음 급여 기간 = 현재 기간 끝 다음날이 속한 기간 */
    suspend fun getNextPayPeriod(currentPeriod: PayPeriod, payday: Int, adjustment: PaydayAdjustment): PayPeriod {
        return getCurrentPayPeriod(payday, adjustment, currentDate = currentPeriod.endDate.plus(1, DateTimeUnit.DAY))
    }

    /** 이전 급여 기간 = 현재 기간 시작 전날이 속한 기간 */
    suspend fun getPreviousPayPeriod(currentPeriod: PayPeriod, payday: Int, adjustment: PaydayAdjustment): PayPeriod {
        return getCurrentPayPeriod(payday, adjustment, currentDate = currentPeriod.startDate.minus(1, DateTimeUnit.DAY))
    }

    suspend fun calculateActualPayday(
        year: Int,
        month: Month,
        payday: Int,
        adjustment: PaydayAdjustment
    ): LocalDate {
        val targetDate = try {
            LocalDate(year, month, payday)
        } catch (e: IllegalArgumentException) {
            // 해당 월에 그 날짜가 없으면 (예: 2월 30일) 마지막 날로 설정
            val lastDayOfMonth = LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
            lastDayOfMonth
        }

        // 주말 또는 공휴일 체크
        val isWeekend = targetDate.dayOfWeek == DayOfWeek.SATURDAY || targetDate.dayOfWeek == DayOfWeek.SUNDAY
        val isHoliday = checkIsHoliday(targetDate)

        return if (isWeekend || isHoliday) {
            when (adjustment) {
                PaydayAdjustment.BEFORE_WEEKEND -> {
                    // 주말/공휴일 이전 평일로 이동
                    var adjustedDate = targetDate
                    while (isWeekendOrHoliday(adjustedDate)) {
                        adjustedDate = adjustedDate.minus(1, DateTimeUnit.DAY)
                    }
                    adjustedDate
                }
                PaydayAdjustment.AFTER_WEEKEND -> {
                    // 주말/공휴일 이후 평일로 이동
                    var adjustedDate = targetDate
                    while (isWeekendOrHoliday(adjustedDate)) {
                        adjustedDate = adjustedDate.plus(1, DateTimeUnit.DAY)
                    }
                    adjustedDate
                }
            }
        } else {
            targetDate // 평일이고 공휴일 아니면 그대로
        }
    }

    private suspend fun isWeekendOrHoliday(date: LocalDate): Boolean {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val isHoliday = checkIsHoliday(date)
        return isWeekend || isHoliday
    }

    private suspend fun checkIsHoliday(date: LocalDate): Boolean {
        if (holidayRepository == null) return false

        // YYYYMMDD 형식으로 변환
        val dateStr = "${date.year}${date.monthNumber.toString().padStart(2, '0')}${date.dayOfMonth.toString().padStart(2, '0')}"
        val holiday = holidayRepository.getHolidayByDate(dateStr)
        return holiday?.isHoliday == true
    }
    
    fun getRecommendedDateForPeriod(payPeriod: PayPeriod, payday: Int, adjustment: PaydayAdjustment): LocalDate {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        return if (today >= payPeriod.startDate && today <= payPeriod.endDate) {
            // 오늘이 기간 내에 있으면 오늘 날짜 반환
            today
        } else {
            // 오늘이 기간 내에 없으면 해당 기간의 월급날 반환
            payPeriod.startDate
        }
    }
    
    private fun formatPayPeriodDisplay(startDate: LocalDate, endDate: LocalDate): String {
        return if (startDate.year == endDate.year && startDate.month == endDate.month) {
            // 같은 월인 경우: "2024년 9월 (25일~24일)"
            "${startDate.year}년 ${startDate.monthNumber}월 (${startDate.dayOfMonth}일~${endDate.dayOfMonth}일)"
        } else if (startDate.year == endDate.year) {
            // 같은 년도, 다른 월: "2024년 8월25일~9월24일"
            "${startDate.year}년 ${startDate.monthNumber}월${startDate.dayOfMonth}일~${endDate.monthNumber}월${endDate.dayOfMonth}일"
        } else {
            // 다른 년도: "2023년12월25일~2024년1월24일"
            "${startDate.year}년${startDate.monthNumber}월${startDate.dayOfMonth}일~${endDate.year}년${endDate.monthNumber}월${endDate.dayOfMonth}일"
        }
    }
}
