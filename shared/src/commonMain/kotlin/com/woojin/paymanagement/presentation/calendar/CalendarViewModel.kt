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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.LocalDate
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

    // 상태 갱신 순서 보장: 거래 목록 변경·기간 이동·날짜 선택이 동시에 돌면 늦게 끝난 오래된 계산이
    // 최신 상태를 덮어써 달력이 깨질 수 있어 한 번에 하나씩, 들어온 순서대로 처리 (Mutex는 FIFO)
    private val stateMutex = Mutex()
    private var transactionsUpdateJob: Job? = null
    // currentPayPeriod를 계산할 때 쓴 급여일 설정 (설정 변경 감지용)
    private var periodSettings: Pair<Int, com.woojin.paymanagement.utils.PaydayAdjustment>? = null

    // 공휴일 받기는 한 번에 하나만. 받아도 데이터가 없는 연도(아직 발표 전 등)는 이번 실행 중 다시 시도 안 함
    private val holidayMutex = Mutex()
    private val attemptedHolidayYears = mutableSetOf<Int>()

    companion object {
        private val HOLIDAY_API_KEY = com.woojin.paymanagement.BuildKonfig.HOLIDAY_API_KEY
        // 올해·내년 공휴일을 다시 받는 주기 (임시공휴일처럼 나중에 지정되는 공휴일 반영)
        private const val HOLIDAY_REFRESH_INTERVAL_MS = 30L * 24 * 60 * 60 * 1000
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
            val currentPayPeriod = stateMutex.withLock {
                val currentPayPeriod = if (initialPayPeriod != null) {
                    // 이전에 보던 기간이 지금 급여일 설정과 다를 수 있으므로(데이터 가져오기 등) 현재 설정으로 다시 계산.
                    // 설정이 그대로면 같은 기간이 나옴
                    val anchor = selectedDate?.takeIf { it.isIn(initialPayPeriod) } ?: initialPayPeriod.startDate
                    payPeriodCalculator.getCurrentPayPeriod(payday, adjustment, currentDate = anchor)
                } else {
                    payPeriodCalculator.getCurrentPayPeriod(payday, adjustment)
                }

                // 선택 날짜가 기간 밖이면 헤더(선택 날짜 기준)와 달력(기간 기준)이 어긋나므로 추천 날짜 사용
                val recommendedDate = selectedDate?.takeIf { it.isIn(currentPayPeriod) }
                    ?: payPeriodCalculator.getRecommendedDateForPeriod(currentPayPeriod, payday, adjustment)

                val isMoneyVisible = getMoneyVisibilityUseCase()

                applyStateLocked(
                    transactions = transactions,
                    payPeriod = currentPayPeriod,
                    selectedDate = recommendedDate,
                    isMoneyVisible = isMoneyVisible
                )
                currentPayPeriod
            }

            // init에서 복원된 공유 모드의 리스너를 급여기간 확정 후 여기서 시작
            if (uiState.isSharedMode) {
                startObservingSharedTransactions(currentPayPeriod.startDate, currentPayPeriod.endDate)
            }
            ensureHolidays()
        }
    }

    fun updateTransactions(transactions: List<Transaction>) {
        // 데이터 가져오기처럼 거래가 연달아 바뀌면 마지막 목록만 반영 (기다리던 이전 갱신은 취소)
        transactionsUpdateJob?.cancel()
        transactionsUpdateJob = coroutineScope.launch {
            stateMutex.withLock { applyStateLocked(transactions = transactions) }
        }
    }

    fun selectDate(date: LocalDate) {
        coroutineScope.launch {
            stateMutex.withLock { applyStateLocked(selectedDate = date) }
        }
    }

    fun navigateToPreviousPeriod() {
        coroutineScope.launch {
            val previousPeriod = stateMutex.withLock {
                // 잠금 안에서 최신 상태를 읽어야 빠르게 넘길 때도 기간이 꼬이지 않음
                val currentPeriod = uiState.currentPayPeriod ?: return@launch
                val currentSelectedDate = uiState.selectedDate ?: return@launch

                // 바로 앞 기간 = 현재 기간 시작 전날이 속한 급여 기간 (현재 급여일 설정 기준)
                val previousPeriod = payPeriodCalculator.getCurrentPayPeriod(
                    payday = payday,
                    adjustment = adjustment,
                    currentDate = currentPeriod.startDate.minus(1, DateTimeUnit.DAY)
                )

                // 현재 선택된 날짜의 일(day)을 유지하면서 월만 이전으로 변경
                val newSelectedDate = try {
                    val previousMonth = currentSelectedDate.minus(1, DateTimeUnit.MONTH)
                    if (previousMonth.isIn(previousPeriod)) previousMonth else previousPeriod.startDate
                } catch (e: Exception) {
                    previousPeriod.startDate
                }

                applyStateLocked(payPeriod = previousPeriod, selectedDate = newSelectedDate)
                previousPeriod
            }
            if (uiState.isSharedMode) {
                startObservingSharedTransactions(previousPeriod.startDate, previousPeriod.endDate)
            }
            ensureHolidays()
        }
    }

    fun navigateToNextPeriod() {
        coroutineScope.launch {
            val nextPeriod = stateMutex.withLock {
                val currentPeriod = uiState.currentPayPeriod ?: return@launch
                val currentSelectedDate = uiState.selectedDate ?: return@launch

                // 바로 다음 기간 = 현재 기간 끝 다음날이 속한 급여 기간 (현재 급여일 설정 기준)
                val nextPeriod = payPeriodCalculator.getCurrentPayPeriod(
                    payday = payday,
                    adjustment = adjustment,
                    currentDate = currentPeriod.endDate.plus(1, DateTimeUnit.DAY)
                )

                // 현재 선택된 날짜의 일(day)을 유지하면서 월만 다음으로 변경
                val newSelectedDate = try {
                    val nextMonth = currentSelectedDate.plus(1, DateTimeUnit.MONTH)
                    if (nextMonth.isIn(nextPeriod)) nextMonth else nextPeriod.startDate
                } catch (e: Exception) {
                    nextPeriod.startDate
                }

                applyStateLocked(payPeriod = nextPeriod, selectedDate = newSelectedDate)
                nextPeriod
            }
            if (uiState.isSharedMode) {
                startObservingSharedTransactions(nextPeriod.startDate, nextPeriod.endDate)
            }
            ensureHolidays()
        }
    }

    /**
     * 날짜가 바뀌었으면(자정 경과, 다음 날 앱 복귀) 오늘 표시를 갱신.
     * 어제가 속한 기간을 보고 있었는데 오늘이 다음 기간이면 오늘이 속한 기간으로 이동.
     * 다른 기간을 보고 있었다면 그대로 둠
     */
    fun onDayChanged() {
        val newToday = todayDate()
        if (newToday == uiState.today) return
        coroutineScope.launch {
            val movedPeriod = stateMutex.withLock {
                val oldToday = uiState.today
                if (newToday == oldToday) return@launch
                val period = uiState.currentPayPeriod
                val selected = uiState.selectedDate
                uiState = uiState.copy(today = newToday)
                if (period == null) return@launch

                if (oldToday.isIn(period) && !newToday.isIn(period)) {
                    // 오늘부터 새 급여 기간 → 오늘이 속한 기간으로 이동하고 오늘 선택
                    val newPeriod = payPeriodCalculator.getCurrentPayPeriod(payday, adjustment, currentDate = newToday)
                    applyStateLocked(payPeriod = newPeriod, selectedDate = newToday)
                    newPeriod
                } else {
                    // 같은 기간이면 어제(=오늘이던 날)를 선택 중일 때만 오늘로 옮김
                    if (selected == oldToday && newToday.isIn(period)) {
                        applyStateLocked(selectedDate = newToday)
                    }
                    null
                }
            }
            if (movedPeriod != null && uiState.isSharedMode) {
                startObservingSharedTransactions(movedPeriod.startDate, movedPeriod.endDate)
            }
            // 새해가 됐거나 갱신 주기가 지났을 수 있으므로 공휴일 확인
            ensureHolidays()
        }
    }

    private fun todayDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

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

    /**
     * 상태 계산·반영. 반드시 stateMutex 안에서 호출 (기본값도 잠금 안에서 읽혀 항상 최신 상태 기준)
     */
    private suspend fun applyStateLocked(
        transactions: List<Transaction> = uiState.transactions,
        payPeriod: PayPeriod? = null,
        selectedDate: LocalDate? = null,
        isMoneyVisible: Boolean = uiState.isMoneyVisible
    ) {
        val settings = payday to adjustment
        var actualPayPeriod = payPeriod ?: uiState.currentPayPeriod ?: payPeriodCalculator.getCurrentPayPeriod(
            payday,
            adjustment
        )
        var actualSelectedDate = selectedDate ?: uiState.selectedDate
            ?: payPeriodCalculator.getRecommendedDateForPeriod(actualPayPeriod, payday, adjustment)

        // 급여일 설정이 바뀌었는데(데이터 가져오기, 급여일 변경) 보고 있던 기간이 예전 설정 기준이면
        // 현재 설정으로 다시 맞춤. 안 그러면 이전/다음 이동 때 기간 길이가 틀어져 달력이 깨짐
        val lastSettings = periodSettings
        if (payPeriod == null && lastSettings != null && lastSettings != settings) {
            val anchor = actualSelectedDate.takeIf { it.isIn(actualPayPeriod) } ?: actualPayPeriod.startDate
            actualPayPeriod = payPeriodCalculator.getCurrentPayPeriod(payday, adjustment, currentDate = anchor)
            if (!actualSelectedDate.isIn(actualPayPeriod)) {
                actualSelectedDate = payPeriodCalculator.getRecommendedDateForPeriod(actualPayPeriod, payday, adjustment)
            }
        }
        periodSettings = settings

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

    private fun LocalDate.isIn(period: PayPeriod): Boolean = this >= period.startDate && this <= period.endDate


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
     * 보고 있는 기간에 필요한 공휴일 데이터 확인
     * - 지금 기간·다음 기간에 걸친 연도 중 없는 연도는 받음
     * - 30일마다 올해·내년을 다시 받아 임시공휴일 등 나중에 지정된 공휴일 반영
     * - 새로 받았으면 지금 기간(월급날 조정)과 빨간 날 표시를 다시 계산
     */
    private fun ensureHolidays() {
        coroutineScope.launch {
            val fetched = holidayMutex.withLock {
                try {
                    val period = uiState.currentPayPeriod ?: return@launch
                    val today = todayDate()

                    // 다음 기간이 해를 넘길 수 있어 끝 연도 +1까지 확인
                    val neededYears = setOf(period.startDate.year, period.endDate.year, period.endDate.year + 1)
                    val missingYears = neededYears.filter { year ->
                        year !in attemptedHolidayYears && holidayRepository.getHolidaysByYear(year).isEmpty()
                    }

                    val now = Clock.System.now().toEpochMilliseconds()
                    val refreshDue = now - preferencesRepository.getHolidaysRefreshedAt() > HOLIDAY_REFRESH_INTERVAL_MS
                    val refreshYears = if (refreshDue) listOf(today.year, today.year + 1) else emptyList()

                    val years = (missingYears + refreshYears).distinct().sorted()
                    if (years.isEmpty()) return@launch

                    attemptedHolidayYears.addAll(missingYears)
                    val result = holidayRepository.fetchAndSaveHolidays(HOLIDAY_API_KEY, years)
                    if (result.isFailure) {
                        // 네트워크 실패 등: 다음 기회(앱 복귀·기간 이동)에 다시 시도
                        attemptedHolidayYears.removeAll(missingYears.toSet())
                        println("공휴일 데이터 받기 실패: ${result.exceptionOrNull()?.message}")
                        return@launch
                    }
                    if (refreshDue) preferencesRepository.setHolidaysRefreshedAt(now)
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("공휴일 데이터 확인 중 오류: ${e.message}")
                    return@launch
                }
            }
            if (fetched) recalculateCurrentPeriod()
        }
    }

    /**
     * 공휴일 데이터가 바뀐 뒤 지금 보고 있는 기간을 다시 계산 (월급날 조정 + 빨간 날 표시)
     */
    private suspend fun recalculateCurrentPeriod() {
        val changedPeriod = stateMutex.withLock {
            val period = uiState.currentPayPeriod ?: return
            // 월급날이 며칠 옮겨져도 같은 기간을 가리키도록 기간 중간 날짜를 기준으로 계산
            val anchor = period.startDate.plus(14, DateTimeUnit.DAY)
            val newPeriod = payPeriodCalculator.getCurrentPayPeriod(payday, adjustment, currentDate = anchor)
            val selected = uiState.selectedDate
            val newSelected = selected?.takeIf { it.isIn(newPeriod) }
                ?: payPeriodCalculator.getRecommendedDateForPeriod(newPeriod, payday, adjustment)
            applyStateLocked(payPeriod = newPeriod, selectedDate = newSelected)
            newPeriod.takeIf { it.startDate != period.startDate || it.endDate != period.endDate }
        }
        if (changedPeriod != null && uiState.isSharedMode) {
            startObservingSharedTransactions(changedPeriod.startDate, changedPeriod.endDate)
        }
    }
}
