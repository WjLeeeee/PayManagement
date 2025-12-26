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

    override suspend fun fetchAndSaveHolidays(serviceKey: String, years: List<Int>): Result<Unit> {
        return try {
            // 공휴일과 국경일 API 엔드포인트
            val apiEndpoints = listOf(
                "getHoliDeInfo" to "공휴일",  // 설날, 추석, 크리스마스 등
                "getRestDeInfo" to "국경일"   // 3.1절, 광복절, 개천절, 한글날
            )

            years.forEach { year ->
                // 해당 연도의 모든 월(1~12) 데이터 가져오기
                (1..12).forEach { month ->
                    val monthStr = month.toString().padStart(2, '0')

                    // 공휴일과 국경일 둘 다 호출
                    apiEndpoints.forEach { (endpoint, type) ->
                        val httpResponse = client.get {
                            url("https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/$endpoint")
                            parameter("serviceKey", serviceKey)
                            parameter("solYear", year.toString())
                            parameter("solMonth", monthStr)
                            parameter("numOfRows", "100")
                            parameter("pageNo", "1")
                            parameter("_type", "json")
                        }

                        // JSON 응답 로그 출력
                        val jsonString = httpResponse.bodyAsText()
                        println("API 응답 JSON ($type ${year}년 ${monthStr}월): $jsonString")

                        // JSON 파싱
                        val response: HolidayApiResponse = json.decodeFromString(jsonString)

                        // API 응답 확인
                        if (response.response.header.resultCode != "00") {
                            println("API 에러 ($type): ${response.response.header.resultMsg}")
                            return@forEach // 이 API는 건너뛰고 다음 진행
                        }

                        // 데이터가 있으면 DB에 저장
                        response.response.body.items?.item?.forEach { item ->
                            if (item.isHoliday == "Y") {  // 공휴일/국경일만 저장
                                println("${year}년 ${monthStr}월 $type: ${item.dateName}")
                                databaseHelper.insertHoliday(
                                    locdate = item.locdate,
                                    dateName = item.dateName,
                                    isHoliday = item.isHoliday,
                                    year = year.toLong()
                                )
                            }
                        } ?: run {
                            // items가 null이거나 빈 경우
                            println("${year}년 ${monthStr}월: $type 없음")
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

    override suspend fun fetchAndSaveHolidaysForMonths(
        serviceKey: String,
        startYear: Int,
        startMonth: Int,
        monthCount: Int
    ): Result<Unit> {
        return try {
            val apiEndpoints = listOf(
                "getHoliDeInfo" to "공휴일",
                "getRestDeInfo" to "국경일"
            )

            var currentYear = startYear
            var currentMonth = startMonth

            repeat(monthCount) {
                val monthStr = currentMonth.toString().padStart(2, '0')

                apiEndpoints.forEach { (endpoint, type) ->
                    val httpResponse = client.get {
                        url("https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/$endpoint")
                        parameter("serviceKey", serviceKey)
                        parameter("solYear", currentYear.toString())
                        parameter("solMonth", monthStr)
                        parameter("numOfRows", "100")
                        parameter("pageNo", "1")
                        parameter("_type", "json")
                    }

                    val jsonString = httpResponse.bodyAsText()
                    println("API 응답 JSON ($type ${currentYear}년 ${monthStr}월): $jsonString")

                    val response: HolidayApiResponse = json.decodeFromString(jsonString)

                    if (response.response.header.resultCode != "00") {
                        println("API 에러 ($type): ${response.response.header.resultMsg}")
                        return@forEach
                    }

                    response.response.body.items?.item?.forEach { item ->
                        if (item.isHoliday == "Y") {
                            println("${currentYear}년 ${monthStr}월 $type: ${item.dateName}")
                            databaseHelper.insertHoliday(
                                locdate = item.locdate,
                                dateName = item.dateName,
                                isHoliday = item.isHoliday,
                                year = currentYear.toLong()
                            )
                        }
                    } ?: run {
                        println("${currentYear}년 ${monthStr}월: $type 없음")
                    }
                }

                // 다음 달로 이동
                currentMonth++
                if (currentMonth > 12) {
                    currentMonth = 1
                    currentYear++
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLatestHolidayDate(): String? {
        return databaseHelper.getLatestHolidayDate()
    }
}
