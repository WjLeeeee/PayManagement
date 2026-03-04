package com.woojin.paymanagement.data.repository

import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.model.Holiday
import com.woojin.paymanagement.domain.model.HolidayApiResponse
import com.woojin.paymanagement.domain.repository.HolidayRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class HolidayRepositoryImpl(
    private val databaseHelper: DatabaseHelper
) : HolidayRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    // 공휴일(설날·추석·크리스마스 등)과 국경일(3.1절·광복절 등) 두 엔드포인트
    private val apiEndpoints = listOf(
        "getHoliDeInfo" to "공휴일",
        "getRestDeInfo" to "국경일"
    )

    override suspend fun fetchAndSaveHolidays(serviceKey: String, years: List<Int>): Result<Unit> {
        return try {
            years.forEach { year ->
                // solMonth 없이 연도 전체를 한 번에 조회 → 연도당 2번 호출 (공휴일 + 국경일)
                apiEndpoints.forEach { (endpoint, _) ->
                    val jsonString = client.get {
                        url("https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/$endpoint")
                        parameter("serviceKey", serviceKey)
                        parameter("solYear", year.toString())
                        parameter("numOfRows", "100")
                        parameter("pageNo", "1")
                        parameter("_type", "json")
                    }.bodyAsText()

                    val response: HolidayApiResponse = json.decodeFromString(jsonString)

                    if (response.response.header.resultCode != "00") {
                        return@forEach
                    }

                    response.response.body.items?.item?.forEach { item ->
                        if (item.isHoliday == "Y") {
                            databaseHelper.insertHoliday(
                                locdate = item.locdate,
                                dateName = item.dateName,
                                isHoliday = item.isHoliday,
                                year = year.toLong()
                            )
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHolidayByDate(date: String): Holiday? {
        val entity = databaseHelper.getHolidayByDate(date) ?: return null
        return Holiday(
            locdate = entity.locdate,
            dateName = entity.dateName,
            isHoliday = entity.isHoliday == "Y",
            year = entity.year.toInt()
        )
    }

    override suspend fun getHolidaysByYear(year: Int): List<Holiday> {
        return databaseHelper.getHolidaysByYear(year.toLong()).map { entity ->
            Holiday(
                locdate = entity.locdate,
                dateName = entity.dateName,
                isHoliday = entity.isHoliday == "Y",
                year = entity.year.toInt()
            )
        }
    }

    override suspend fun deleteHolidaysByYear(year: Int) {
        databaseHelper.deleteHolidaysByYear(year.toLong())
    }

    override suspend fun getLatestHolidayDate(): String? {
        return databaseHelper.getLatestHolidayDate()
    }
}
