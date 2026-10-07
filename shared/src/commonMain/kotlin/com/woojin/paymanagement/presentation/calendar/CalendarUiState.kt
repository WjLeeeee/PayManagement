package com.woojin.paymanagement.presentation.calendar

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.model.SharedTransaction
import com.woojin.paymanagement.utils.PayPeriod
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class CalendarUiState(
    val isLoading: Boolean = false,
    val currentPayPeriod: PayPeriod? = null,
    val selectedDate: LocalDate? = null,
    // 오늘 날짜 (앱을 켜 둔 채 날짜가 바뀌면 갱신됨)
    val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val transactions: List<Transaction> = emptyList(),
    val payPeriodSummary: PayPeriodSummary = PayPeriodSummary(),
    val dailyTransactions: List<Transaction> = emptyList(),
    val isMoneyVisible: Boolean = true,
    val error: String? = null,
    val isMoveMode: Boolean = false,
    val transactionToMove: Transaction? = null,
    val availableCategories: List<Category> = emptyList(),
    val holidays: Set<LocalDate> = emptySet(),
    val holidayNames: Map<LocalDate, String> = emptyMap(),
    // 공유 모드
    val isInSharedRoom: Boolean = false,
    val isSharedMode: Boolean = false,
    val sharedTransactions: List<SharedTransaction> = emptyList(),
    val sharedError: String? = null
)

data class PayPeriodSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val transactionCount: Int = 0
)