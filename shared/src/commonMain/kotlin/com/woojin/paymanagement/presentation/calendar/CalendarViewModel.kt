package com.woojin.paymanagement.presentation.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.repository.PreferencesRepository
import com.woojin.paymanagement.domain.repository.SharedModeManager
import com.woojin.paymanagement.domain.repository.SharedRoomRepository
import com.woojin.paymanagement.domain.usecase.GetDailyTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.GetPayPeriodSummaryUseCase
import com.woojin.paymanagement.domain.usecase.GetMoneyVisibilityUseCase
import com.woojin.paymanagement.domain.usecase.SetMoneyVisibilityUseCase
import com.woojin.paymanagement.domain.usecase.UpdateTransactionUseCase
import com.woojin.paymanagement.domain.usecase.GetCategoriesUseCase
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.PayPeriodCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    private val coroutineScope: CoroutineScope,
    private val sharedRoomRepository: SharedRoomRepository? = null
) {
    var uiState by mutableStateOf(CalendarUiState())
        private set

    private val payday: Int get() = preferencesRepository.getPayday()
    private val adjustment: com.woojin.paymanagement.utils.PaydayAdjustment get() = preferencesRepository.getPaydayAdjustment()

    private var sharedTransactionJob: Job? = null
    private var observedStartDate: LocalDate? = null
    private var observedEndDate: LocalDate? = null

    companion object {
        private val HOLIDAY_API_KEY = com.woojin.paymanagement.BuildKonfig.HOLIDAY_API_KEY
    }

    init {
        // 공유방 참여 여부 확인 및 이전 공유 모드 상태 복원
        if (sharedRoomRepository != null) {
            coroutineScope.launch {
                val room = runCatching { sharedRoomRepository.getCurrentRoom() }.getOrNull()
                if (room != null) {
                    SharedModeManager.myDeviceId = sharedRoomRepository.getDeviceId()
                    SharedModeManager.sharedRoomId = room.roomId
                    uiState = uiState.copy(isInSharedRoom = true)
                    // 앱 재시작 후 공유 모드 상태 복원 (PreferencesRepository에서)
                    val savedSharedMode = preferencesRepository.isSharedMode()
                    if (savedSharedMode) {
                        SharedModeManager.isSharedMode = true
                        uiState = uiState.copy(isSharedMode = true)
                        // initializeCalendar가 먼저 실행된 경우 isSharedMode=false로 리스너를 못 시작했을 수 있음.
                        // 이 시점에 급여기간이 이미 알려져 있으면 즉시 리스너를 시작하고,
                        // 아직 모르면 현재 날짜 기준으로 직접 계산해서 시작.
                        val payPeriod = uiState.currentPayPeriod
                            ?: payPeriodCalculator.getCurrentPayPeriod(payday, adjustment)
                        startObservingSharedTransactions(payPeriod.startDate, payPeriod.endDate)
                    }
                } else {
                    // 공유방 없으면 SharedModeManager 및 저장 상태 초기화
                    SharedModeManager.sharedRoomId = null
                    SharedModeManager.isSharedMode = false
                    SharedModeManager.cachedSharedTransactions = emptyList()
                    preferencesRepository.setIsSharedMode(false)
                }
            }
        }

        // 카테고리 목록을 로드하여 UiState에 반영
        coroutineScope.launch {
            combine(
                getCategoriesUseCase(TransactionType.INCOME),
                getCategoriesUseCase(TransactionType.EXPENSE),
                getCategoriesUseCase(TransactionType.SAVING),
                getCategoriesUseCase(TransactionType.INVESTMENT)
            ) { income, expense, saving, investment ->
                income + expense + saving + investment
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

            // init에서 복원된 공유 모드의 리스너를 급여기간 확정 후 여기서 시작
            if (uiState.isSharedMode) {
                startObservingSharedTransactions(currentPayPeriod.startDate, currentPayPeriod.endDate)
            }
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
            if (uiState.isSharedMode) {
                startObservingSharedTransactions(previousPeriod.startDate, previousPeriod.endDate)
            }
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
            if (uiState.isSharedMode) {
                startObservingSharedTransactions(nextPeriod.startDate, nextPeriod.endDate)
            }
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
            if (uiState.isSharedMode) {
                startObservingSharedTransactions(targetPayPeriod.startDate, targetPayPeriod.endDate)
            }
        }
    }

    fun refreshSharedRoomState() {
        if (sharedRoomRepository == null) return
        coroutineScope.launch {
            val room = runCatching { sharedRoomRepository.getCurrentRoom() }.getOrNull()
            if (room == null) {
                sharedTransactionJob?.cancel()
                sharedTransactionJob = null
                SharedModeManager.isSharedMode = false
                SharedModeManager.sharedRoomId = null
                SharedModeManager.cachedSharedTransactions = emptyList()
                preferencesRepository.setIsSharedMode(false)
                uiState = uiState.copy(
                    isInSharedRoom = false,
                    isSharedMode = false,
                    sharedTransactions = emptyList()
                )
            }
        }
    }

    fun toggleSharedMode() {
        val newMode = !uiState.isSharedMode
        SharedModeManager.isSharedMode = newMode
        preferencesRepository.setIsSharedMode(newMode)
        uiState = uiState.copy(isSharedMode = newMode)

        if (newMode) {
            val payPeriod = uiState.currentPayPeriod ?: return
            startObservingSharedTransactions(payPeriod.startDate, payPeriod.endDate)
        } else {
            sharedTransactionJob?.cancel()
            sharedTransactionJob = null
            uiState = uiState.copy(sharedTransactions = emptyList())
        }
    }

    fun clearSharedError() {
        uiState = uiState.copy(sharedError = null)
    }

    private fun startObservingSharedTransactions(startDate: LocalDate, endDate: LocalDate) {
        val roomId = SharedModeManager.sharedRoomId ?: return
        val repo = sharedRoomRepository ?: return

        // 동일 기간을 이미 감지 중이면 리스너를 재시작하지 않음 (화면 복귀 시 불필요한 재구독 방지)
        if (sharedTransactionJob?.isActive == true &&
            observedStartDate == startDate &&
            observedEndDate == endDate) return

        observedStartDate = startDate
        observedEndDate = endDate
        sharedTransactionJob?.cancel()
        sharedTransactionJob = coroutineScope.launch {
            repo.observeTransactions(roomId, startDate, endDate).collect { result ->
                result.onSuccess { sharedList ->
                    SharedModeManager.cachedSharedTransactions = sharedList
                    uiState = uiState.copy(sharedTransactions = sharedList, sharedError = null)
                }.onFailure {
                    if (SharedModeManager.cachedSharedTransactions.isEmpty()) {
                        // 캐시도 없고 연결도 안 됨 → 토글 되돌리기
                        SharedModeManager.isSharedMode = false
                        preferencesRepository.setIsSharedMode(false)
                        uiState = uiState.copy(
                            isSharedMode = false,
                            sharedTransactions = emptyList(),
                            sharedError = "인터넷 연결을 확인해주세요."
                        )
                        sharedTransactionJob?.cancel()
                    }
                    // 캐시가 있으면 기존 데이터 유지 (아무것도 안 함)
                }
            }
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
            }.toMutableList()

            // 근로자의 날(5월 1일)은 별도 법률에 근거하여 공휴일 API에 포함되지 않으므로 직접 추가
            years.forEach { year ->
                val laborDay = LocalDate(year, 5, 1)
                if (laborDay >= payPeriod.startDate && laborDay <= payPeriod.endDate) {
                    if (holidayMap.none { it.first == laborDay }) {
                        holidayMap.add(laborDay to "근로자의 날")
                    }
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
     * 현재 보는 연도 또는 다음 연도 데이터가 없으면 해당 연도를 추가 로드
     */
    private fun checkAndLoadHolidays(currentViewDate: LocalDate) {
        coroutineScope.launch {
            try {
                val yearsToCheck = listOf(currentViewDate.year, currentViewDate.year + 1)
                val missingYears = yearsToCheck.filter { year ->
                    holidayRepository.getHolidaysByYear(year).isEmpty()
                }

                if (missingYears.isNotEmpty()) {
                    holidayRepository.fetchAndSaveHolidays(HOLIDAY_API_KEY, missingYears).onFailure { error ->
                        println("공휴일 자동 로딩 실패: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                println("공휴일 자동 로딩 체크 중 오류: ${e.message}")
            }
        }
    }
}