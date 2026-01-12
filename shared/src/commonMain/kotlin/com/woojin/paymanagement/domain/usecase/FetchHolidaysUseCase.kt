package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.HolidayRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FetchHolidaysUseCase(
    private val holidayRepository: HolidayRepository
) {
    suspend operator fun invoke(serviceKey: String): Result<Unit> {
        // 이전 1년 + 현재 연도 + 다음 1년 데이터 가져오기
        val currentYear = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .year

        val targetYears = listOf(currentYear - 1, currentYear, currentYear + 1)

        // 각 년도별로 DB에 데이터가 있는지 확인
        val missingYears = targetYears.filter { year ->
            val existingHolidays = holidayRepository.getHolidaysByYear(year)
            existingHolidays.isEmpty() // 데이터가 없는 년도만 필터링
        }

        // 모든 년도의 데이터가 이미 있으면 API 호출 스킵
        if (missingYears.isEmpty()) {
            return Result.success(Unit)
        }

        // 없는 년도만 API 호출
        return holidayRepository.fetchAndSaveHolidays(serviceKey, missingYears)
    }
}
