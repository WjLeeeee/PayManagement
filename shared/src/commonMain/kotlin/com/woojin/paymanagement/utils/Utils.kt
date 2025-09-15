package com.woojin.paymanagement.utils

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

object PayPeriodCalculator {
    
    fun getCurrentPayPeriod(
        payday: Int,
        adjustment: PaydayAdjustment,
        currentDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): PayPeriod {
        // 현재 달의 월급날 계산
        val currentMonthPayday = calculateActualPayday(currentDate.year, currentDate.month, payday, adjustment)
        
        return if (currentDate >= currentMonthPayday) {
            // 이번 달 월급날이 지났으면, 이번 달 월급날 ~ 다음 달 월급날 전날
            val nextMonthDate = currentDate.plus(1, DateTimeUnit.MONTH)
            val nextMonthPayday = calculateActualPayday(
                nextMonthDate.year, 
                nextMonthDate.month, 
                payday, 
                adjustment
            )
            PayPeriod(
                startDate = currentMonthPayday,
                endDate = nextMonthPayday.minus(1, DateTimeUnit.DAY),
                displayText = formatPayPeriodDisplay(currentMonthPayday, nextMonthPayday.minus(1, DateTimeUnit.DAY))
            )
        } else {
            // 이번 달 월급날이 아직 안 왔으면, 지난 달 월급날 ~ 이번 달 월급날 전날
            val previousMonthDate = currentDate.minus(1, DateTimeUnit.MONTH)
            val previousMonthPayday = calculateActualPayday(
                previousMonthDate.year, 
                previousMonthDate.month, 
                payday, 
                adjustment
            )
            PayPeriod(
                startDate = previousMonthPayday,
                endDate = currentMonthPayday.minus(1, DateTimeUnit.DAY),
                displayText = formatPayPeriodDisplay(previousMonthPayday, currentMonthPayday.minus(1, DateTimeUnit.DAY))
            )
        }
    }
    
    fun getNextPayPeriod(currentPeriod: PayPeriod, payday: Int, adjustment: PaydayAdjustment): PayPeriod {
        val nextStartDate = currentPeriod.endDate.plus(1, DateTimeUnit.DAY)
        val nextMonthDate = nextStartDate.plus(1, DateTimeUnit.MONTH)
        val nextEndDate = calculateActualPayday(
            nextMonthDate.year, 
            nextMonthDate.month, 
            payday, 
            adjustment
        ).minus(1, DateTimeUnit.DAY)
        
        return PayPeriod(
            startDate = nextStartDate,
            endDate = nextEndDate,
            displayText = formatPayPeriodDisplay(nextStartDate, nextEndDate)
        )
    }
    
    fun getPreviousPayPeriod(currentPeriod: PayPeriod, payday: Int, adjustment: PaydayAdjustment): PayPeriod {
        val previousEndDate = currentPeriod.startDate.minus(1, DateTimeUnit.DAY)
        val previousMonthDate = currentPeriod.startDate.minus(1, DateTimeUnit.MONTH)
        val previousStartDate = calculateActualPayday(
            previousMonthDate.year, 
            previousMonthDate.month, 
            payday, 
            adjustment
        )
        
        return PayPeriod(
            startDate = previousStartDate,
            endDate = previousEndDate,
            displayText = formatPayPeriodDisplay(previousStartDate, previousEndDate)
        )
    }
    
    private fun calculateActualPayday(
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
        
        return when (targetDate.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> {
                when (adjustment) {
                    PaydayAdjustment.BEFORE_WEEKEND -> {
                        // 주말 이전 평일로 이동
                        var adjustedDate = targetDate
                        while (adjustedDate.dayOfWeek == DayOfWeek.SATURDAY || adjustedDate.dayOfWeek == DayOfWeek.SUNDAY) {
                            adjustedDate = adjustedDate.minus(1, DateTimeUnit.DAY)
                        }
                        adjustedDate
                    }
                    PaydayAdjustment.AFTER_WEEKEND -> {
                        // 주말 이후 평일로 이동
                        var adjustedDate = targetDate
                        while (adjustedDate.dayOfWeek == DayOfWeek.SATURDAY || adjustedDate.dayOfWeek == DayOfWeek.SUNDAY) {
                            adjustedDate = adjustedDate.plus(1, DateTimeUnit.DAY)
                        }
                        adjustedDate
                    }
                }
            }
            else -> targetDate // 평일이면 그대로
        }
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
