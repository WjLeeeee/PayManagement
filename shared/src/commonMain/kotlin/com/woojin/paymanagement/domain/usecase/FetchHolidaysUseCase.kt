package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.HolidayRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FetchHolidaysUseCase(
    private val holidayRepository: HolidayRepository
) {
    suspend operator fun invoke(serviceKey: String): Result<Unit> {
        // 현재 연도부터 향후 1년치 데이터 가져오기
        val currentYear = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .year

        val years = listOf(currentYear, currentYear + 1)

        return holidayRepository.fetchAndSaveHolidays(serviceKey, years)
    }
}
