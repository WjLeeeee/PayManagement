package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.HolidayRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FetchHolidaysUseCase(
    private val holidayRepository: HolidayRepository
) {
    suspend operator fun invoke(serviceKey: String): Result<Unit> {
        val currentYear = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .year

        val targetYears = listOf(currentYear - 1, currentYear, currentYear + 1)

        // 데이터가 없는 연도만 호출
        val missingYears = targetYears.filter { year ->
            holidayRepository.getHolidaysByYear(year).isEmpty()
        }

        if (missingYears.isEmpty()) {
            return Result.success(Unit)
        }

        return holidayRepository.fetchAndSaveHolidays(serviceKey, missingYears)
    }
}
