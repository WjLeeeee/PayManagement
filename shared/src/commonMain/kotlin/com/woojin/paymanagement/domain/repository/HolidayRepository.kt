package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.domain.model.Holiday

interface HolidayRepository {
    suspend fun fetchAndSaveHolidays(serviceKey: String, years: List<Int>): Result<Unit>
    suspend fun getHolidayByDate(date: String): Holiday?
    suspend fun getHolidaysByYear(year: Int): List<Holiday>
    suspend fun deleteHolidaysByYear(year: Int)
    suspend fun getLatestHolidayDate(): String?
}
