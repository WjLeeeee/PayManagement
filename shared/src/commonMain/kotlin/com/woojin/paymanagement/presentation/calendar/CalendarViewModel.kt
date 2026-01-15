package com.woojin.paymanagement.presentation.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.repository.PreferencesRepository
import com.woojin.paymanagement.domain.usecase.GetDailyTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.GetPayPeriodSummaryUseCase
import com.woojin.paymanagement.domain.usecase.GetMoneyVisibilityUseCase
import com.woojin.paymanagement.domain.usecase.SetMoneyVisibilityUseCase
import com.woojin.paymanagement.domain.usecase.UpdateTransactionUseCase
import com.woojin.paymanagement.domain.usecase.GetCategoriesUseCase
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.PayPeriodCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class CalendarViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val getPayPeriodSummaryUseCase: GetPayPeriodSummaryUseCase,
    private val getDailyTransactionsUseCase: GetDailyTransactionsUseCase,
    private val getMoneyVisibilityUseCase: GetMoneyVisibilityUseCase,
    private val setMoneyVisibilityUseCase: SetMoneyVisibilityUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val payPeriodCalculator: PayPeriodCalculator,
    private val holidayRepository: com.woojin.paymanagement.domain.repository.HolidayRepository,
    private val coroutineScope: CoroutineScope
) {
    var uiState by mutableStateOf(CalendarUiState())
        private set

    private val payday: Int get() = preferencesRepository.getPayday()
    private val adjustment: com.woojin.paymanagement.utils.PaydayAdjustment get() = preferencesRepository.getPaydayAdjustment()

    companion object {
        private val HOLIDAY_API_KEY = com.woojin.paymanagement.BuildKonfig.HOLIDAY_API_KEY
    }

    init {
        // 카테고리 목록을 로드하여 UiState에 반영
        coroutineScope.launch {
            combine(
                getCategoriesUseCase(TransactionType.INCOME),
                getCategoriesUseCase(TransactionType.EXPENSE)
            ) { income, expense ->
                income + expense
            }.collect { categories ->
                uiState = uiState.copy(availableCategories = categories)
            }
        }
    }

    fun initializeCalendar(
        transactions: List<Transaction>,
        initialPayPeriod: PayPeriod? = null,
        selectedDate: LocalDate? = null
    ) {
        coroutineScope.launch {
            val currentPayPeriod = initialPayPeriod
                ?: payPeriodCalculator.getCurrentPayPeriod(payday, adjustment)

            val recommendedDate = selectedDate
                ?: payPeriodCalculator.getRecommendedDateForPeriod(currentPayPeriod, payday, adjustment)

            val isMoneyVisible = getMoneyVisibilityUseCase()

            updateState(
                transactions = transactions,
                payPeriod = currentPayPeriod,
                selectedDate = recommendedDate,
                isMoneyVisible = isMoneyVisible
            )
        }
    }

    fun updateTransactions(transactions: List<Transaction>) {
        updateState(transactions = transactions)
    }

    fun selectDate(date: LocalDate) {
        updateState(selectedDate = date)
    }

    fun navigateToPreviousPeriod() {
        val currentSelectedDate = uiState.selectedDate ?: return
        coroutineScope.launch {
            val previousPeriod = payPeriodCalculator.getPreviousPayPeriod(
                currentPeriod = requireNotNull(uiState.currentPayPeriod),
                payday = payday,
                adjustment = adjustment
            )

        // 현재 선택된 날짜의 일(day)을 유지하면서 월만 이전으로 변경
        val newSelectedDate = try {
            // 이전 달로 이동하면서 같은 일(day) 유지
            val previousMonth = currentSelectedDate.minus(1, DateTimeUnit.MONTH)
            // 새 급여 기간 내에서 유효한 날짜인지 확인
            if (previousMonth >= previousPeriod.startDate && previousMonth <= previousPeriod.endDate) {
                previousMonth
            } else {
                // 기간 밖이면 급여일 선택
                previousPeriod.startDate
            }
        } catch (e: Exception) {
            // 날짜가 유효하지 않으면 (예: 1월 31일 → 2월 31일) 급여일 선택
            previousPeriod.startDate
        }

            updateState(
                payPeriod = previousPeriod,
                selectedDate = newSelectedDate
            )
        }
    }

    fun navigateToNextPeriod() {
        val currentSelectedDate = uiState.selectedDate ?: return
        coroutineScope.launch {
            val nextPeriod = payPeriodCalculator.getNextPayPeriod(
                currentPeriod = requireNotNull(uiState.currentPayPeriod),
                payday = payday,
                adjustment = adjustment
            )

            // 공휴일 자동 로딩 체크
            checkAndLoadHolidays(nextPeriod.endDate)

        // 현재 선택된 날짜의 일(day)을 유지하면서 월만 다음으로 변경
        val newSelectedDate = try {
            // 다음 달로 이동하면서 같은 일(day) 유지
            val nextMonth = currentSelectedDate.plus(1, DateTimeUnit.MONTH)
            // 새 급여 기간 내에서 유효한 날짜인지 확인
            if (nextMonth >= nextPeriod.startDate && nextMonth <= nextPeriod.endDate) {
                nextMonth
            } else {
                // 기간 밖이면 급여일 선택
                nextPeriod.startDate
            }
        } catch (e: Exception) {
            // 날짜가 유효하지 않으면 (예: 1월 31일 → 2월 31일) 급여일 선택
            nextPeriod.startDate
        }

            updateState(
                payPeriod = nextPeriod,
                selectedDate = newSelectedDate
            )
        }
    }

    /**
     * 특정 년/월의 급여일로 이동
     * 예: 2025년 12월 선택 시 → 12월 25일~1월 24일 급여 기간
     */
    fun navigateToYearMonth(year: Int, month: Int) {
        coroutineScope.launch {
            // 선택한 년/월의 실제 급여일 계산 (주말 조정 포함)
            val targetPayday = payPeriodCalculator.calculateActualPayday(
                year = year,
                month = Month(month),
                payday = payday,
                adjustment = adjustment
            )

            // 해당 급여일을 기준으로 급여 기간 계산
            // getCurrentPayPeriod는 전달된 날짜가 급여일이면 그날부터 다음 급여일까지의 기간을 반환
            val targetPayPeriod = payPeriodCalculator.getCurrentPayPeriod(
                payday = payday,
                adjustment = adjustment,
                currentDate = targetPayday
            )

            // 급여일을 선택 날짜로 설정
            updateState(
                payPeriod = targetPayPeriod,
                selectedDate = targetPayday
            )
        }
    }

    fun toggleMoneyVisibility() {
        val newVisibility = !uiState.isMoneyVisible
        setMoneyVisibilityUseCase(newVisibility)
        uiState = uiState.copy(isMoneyVisible = newVisibility)
    }

    fun startMoveMode(transaction: Transaction) {
        uiState = uiState.copy(
            isMoveMode = true,
            transactionToMove = transaction
        )
    }

    fun cancelMoveMode() {
        uiState = uiState.copy(
            isMoveMode = false,
            transactionToMove = null
        )
    }

    fun moveTransactionToDate(newDate: LocalDate) {
        val transaction = uiState.transactionToMove ?: return

        coroutineScope.launch {
            try {
                // 거래의 날짜를 새로운 날짜로 업데이트
                val updatedTransaction = transaction.copy(date = newDate)
                updateTransactionUseCase(updatedTransaction)

                // 이동 모드 종료
                uiState = uiState.copy(
                    isMoveMode = false,
                    transactionToMove = null
                )
            } catch (e: Exception) {
                // 에러 처리
                uiState = uiState.copy(
                    isMoveMode = false,
                    transactionToMove = null,
                    error = "거래 이동 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }

    private fun updateState(
        transactions: List<Transaction> = uiState.transactions,
        payPeriod: PayPeriod? = null,
        selectedDate: LocalDate? = null,
        isMoneyVisible: Boolean = uiState.isMoneyVisible
    ) {
        coroutineScope.launch {
            val actualPayPeriod = payPeriod ?: uiState.currentPayPeriod ?: payPeriodCalculator.getCurrentPayPeriod(
                payday,
                adjustment
            )
            val actualSelectedDate = selectedDate ?: uiState.selectedDate
                ?: payPeriodCalculator.getRecommendedDateForPeriod(actualPayPeriod, payday, adjustment)

            val payPeriodSummary = getPayPeriodSummaryUseCase(transactions, actualPayPeriod)
            val dailyTransactions = getDailyTransactionsUseCase(transactions, actualSelectedDate)

            // 공휴일 정보 가져오기
            val holidayInfo = getHolidaysForPayPeriod(actualPayPeriod)

            uiState = uiState.copy(
                currentPayPeriod = actualPayPeriod,
                selectedDate = actualSelectedDate,
                transactions = transactions,
                payPeriodSummary = payPeriodSummary,
                dailyTransactions = dailyTransactions,
                isMoneyVisible = isMoneyVisible,
                holidays = holidayInfo.dates,
                holidayNames = holidayInfo.names
            )
        }
    }

    /**
     * 급여 기간에 해당하는 공휴일 목록 가져오기
     */
    private suspend fun getHolidaysForPayPeriod(payPeriod: PayPeriod): HolidayInfo {
        return try {
            // 급여 기간에 포함된 연도 추출
            val years = setOf(payPeriod.startDate.year, payPeriod.endDate.year)

            // 각 연도의 공휴일 가져오기
            val allHolidays = years.flatMap { year ->
                holidayRepository.getHolidaysByYear(year)
            }

            // YYYYMMDD 형식을 LocalDate로 변환하고 급여 기간 내에 있는 것만 필터링
            val holidayMap = allHolidays.mapNotNull { holiday ->
                try {
                    val year = holiday.locdate.substring(0, 4).toInt()
                    val month = holiday.locdate.substring(4, 6).toInt()
                    val day = holiday.locdate.substring(6, 8).toInt()
                    val date = LocalDate(year, month, day)

                    if (date >= payPeriod.startDate && date <= payPeriod.endDate && holiday.isHoliday) {
                        date to holiday.dateName
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            HolidayInfo(
                dates = holidayMap.map { it.first }.toSet(),
                names = holidayMap.toMap()
            )
        } catch (e: Exception) {
            HolidayInfo(emptySet(), emptyMap())
        }
    }

    private data class HolidayInfo(
        val dates: Set<LocalDate>,
        val names: Map<LocalDate, String>
    )

    /**
     * 공휴일 자동 로딩 체크
     * 현재 보는 날짜가 마지막 저장 날짜 - 2개월 이내면 6개월치 추가 로드
     */
    private fun checkAndLoadHolidays(currentViewDate: LocalDate) {
        coroutineScope.launch {
            try {
                // DB에서 마지막 저장된 공휴일 날짜 조회
                val latestDateStr = holidayRepository.getLatestHolidayDate() ?: return@launch

                // YYYYMMDD 형식을 LocalDate로 변환
                val latestYear = latestDateStr.substring(0, 4).toInt()
                val latestMonth = latestDateStr.substring(4, 6).toInt()
                val latestDay = latestDateStr.substring(6, 8).toInt()
                val latestDate = LocalDate(latestYear, latestMonth, latestDay)

                // 마지막 저장 날짜 - 2개월
                val thresholdDate = latestDate.minus(2, DateTimeUnit.MONTH)

                // 현재 보는 날짜가 임계값 이후면 추가 로딩
                if (currentViewDate >= thresholdDate) {
                    println("공휴일 자동 로딩: 현재 날짜 $currentViewDate, 마지막 저장 $latestDate")

                    // 마지막 저장 날짜의 다음 달부터 6개월치 로드
                    val nextMonth = latestDate.plus(1, DateTimeUnit.MONTH)
                    holidayRepository.fetchAndSaveHolidaysForMonths(
                        serviceKey = HOLIDAY_API_KEY,
                        startYear = nextMonth.year,
                        startMonth = nextMonth.monthNumber,
                        monthCount = 6
                    ).onSuccess {
                        println("공휴일 6개월치 추가 로딩 완료")
                    }.onFailure { error ->
                        println("공휴일 자동 로딩 실패: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                println("공휴일 자동 로딩 체크 중 오류: ${e.message}")
            }
        }
    }
}