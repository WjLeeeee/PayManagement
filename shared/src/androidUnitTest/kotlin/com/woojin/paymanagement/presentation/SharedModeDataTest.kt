package com.woojin.paymanagement.presentation

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.repository.CategoryRepositoryImpl
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.database.PayManagementDatabase
import com.woojin.paymanagement.domain.model.SharedRoom
import com.woojin.paymanagement.domain.model.SharedTransaction
import com.woojin.paymanagement.domain.repository.PreferencesRepository
import com.woojin.paymanagement.domain.repository.SharedModeManager
import com.woojin.paymanagement.domain.repository.SharedRoomRepository
import com.woojin.paymanagement.presentation.monthlycomparison.MonthlyComparisonViewModel
import com.woojin.paymanagement.presentation.search.SearchViewModel
import com.woojin.paymanagement.utils.PayPeriodCalculator
import com.woojin.paymanagement.utils.PaydayAdjustment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 공유 모드에서 월별 비교·검색이 달력 캐시(한 급여 기간)가 아니라 필요한 기간의 공유 거래를 써야 함
 */
class SharedModeDataTest {

    private val db = DatabaseHelper(
        PayManagementDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PayManagementDatabase.Schema.create(it) })
    )
    private val calculator = PayPeriodCalculator()
    // ViewModel이 DB Flow를 계속 구독하므로 실제 스코프를 쓰고 테스트 끝에 취소
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 비동기 로드가 끝나 조건이 맞을 때까지 최대 5초 기다림 */
    private fun waitUntil(condition: () -> Boolean) = runBlocking {
        withTimeoutOrNull(5_000) { while (!condition()) delay(20) }
    }

    @BeforeTest
    fun enterSharedMode() {
        SharedModeManager.isSharedMode = true
        SharedModeManager.sharedRoomId = "room"
        SharedModeManager.cachedSharedTransactions = emptyList() // 달력이 다른 기간을 보고 있어 캐시엔 없음
    }

    @AfterTest
    fun leaveSharedMode() {
        scope.cancel()
        SharedModeManager.isSharedMode = false
        SharedModeManager.sharedRoomId = null
    }

    private fun expense(id: String, amount: Double, date: LocalDate, merchant: String = "가게") = Transaction(
        id = id, amount = amount, type = TransactionType.EXPENSE, category = "식비", merchant = merchant, memo = "", date = date
    )

    @Test
    fun monthlyComparisonUsesSharedTransactionsOfBothPeriods() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val current = runBlocking { calculator.getCurrentPayPeriod(25, PaydayAdjustment.BEFORE_WEEKEND, today) }
        val previous = runBlocking { calculator.getPreviousPayPeriod(current, 25, PaydayAdjustment.BEFORE_WEEKEND) }
        val shared = FakeSharedRoom(
            listOf(expense("prev", 30_000.0, previous.startDate), expense("cur", 10_000.0, current.startDate))
        )

        val viewModel = MonthlyComparisonViewModel(db, CategoryRepositoryImpl(db), Payday25, calculator, scope, shared)
        waitUntil { viewModel.uiState.totalPreviousMonth > 0 }

        assertEquals(10_000.0, viewModel.uiState.totalCurrentMonth)
        assertEquals(30_000.0, viewModel.uiState.totalPreviousMonth)
    }

    @Test
    fun searchFindsSharedTransactionsOutsideCalendarPeriod() {
        val shared = FakeSharedRoom(listOf(expense("old", 4_500.0, LocalDate(2024, 3, 1), merchant = "스타벅스")))

        val viewModel = SearchViewModel(db, CategoryRepositoryImpl(db), scope, shared)
        viewModel.onKeywordChange("스타벅스")
        waitUntil { viewModel.uiState.results.isNotEmpty() }

        assertEquals(listOf("old"), viewModel.uiState.results.map { it.id })
    }

    /** 요청한 기간 안의 거래만 돌려주는 가짜 공유방 */
    private class FakeSharedRoom(private val all: List<Transaction>) : SharedRoomRepository {
        override fun observeTransactions(roomId: String, startDate: LocalDate, endDate: LocalDate): Flow<Result<List<SharedTransaction>>> =
            flowOf(Result.success(all.filter { it.date in startDate..endDate }.map { SharedTransaction(it, "device", true) }))
        override fun getDeviceId() = TODO()
        override suspend fun createRoom(nickname: String): SharedRoom = TODO()
        override suspend fun joinRoom(roomCode: String, nickname: String): SharedRoom? = TODO()
        override suspend fun leaveRoom() = TODO()
        override suspend fun getCurrentRoom(): SharedRoom? = TODO()
        override suspend fun addTransaction(roomId: String, transaction: Transaction) = TODO()
        override suspend fun updateTransaction(roomId: String, transaction: Transaction) = TODO()
        override suspend fun deleteTransaction(roomId: String, transactionId: String) = TODO()
    }

    private object Payday25 : PreferencesRepository {
        override fun getPayday() = 25
        override fun getPaydayAdjustment() = PaydayAdjustment.BEFORE_WEEKEND
        override fun setPayday(day: Int) = TODO()
        override fun setPaydayAdjustment(adjustment: PaydayAdjustment) = TODO()
        override fun isMoneyVisible() = TODO()
        override fun setMoneyVisible(visible: Boolean) = TODO()
        override fun getMonthlySalary() = TODO()
        override fun setMonthlySalary(salary: Double) = TODO()
        override fun isSharedMode() = TODO()
        override fun setIsSharedMode(enabled: Boolean) = TODO()
        override fun getHolidaysRefreshedAt() = TODO()
        override fun setHolidaysRefreshedAt(time: Long) = TODO()
    }
}
