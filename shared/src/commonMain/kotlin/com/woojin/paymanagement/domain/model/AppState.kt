package com.woojin.paymanagement.domain.model

import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.utils.PayPeriod
import kotlinx.datetime.LocalDate

/**
 * 앱 전체 상태를 관리하는 도메인 모델
 */
data class AppState(
    val isKoinInitialized: Boolean = false,
    val navigationState: NavigationState = NavigationState(),
    val appData: AppData = AppData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 네비게이션 관련 상태
 */
data class NavigationState(
    val currentScreen: Screen = Screen.Calendar,
    val previousScreen: Screen = Screen.Calendar,
    val selectedDate: LocalDate? = null,
    val editTransaction: Transaction? = null,
    val selectedPayPeriod: PayPeriod? = null,
    val currentCalendarPayPeriod: PayPeriod? = null
)

/**
 * 앱 데이터 상태
 */
data class AppData(
    val transactions: List<Transaction> = emptyList(),
    val availableBalanceCards: List<BalanceCard> = emptyList(),
    val availableGiftCards: List<GiftCard> = emptyList()
)

/**
 * 화면 열거형
 */
enum class Screen {
    PaydaySetup,
    Calendar,
    Statistics,
    AddTransaction,
    DateDetail,
    EditTransaction
}