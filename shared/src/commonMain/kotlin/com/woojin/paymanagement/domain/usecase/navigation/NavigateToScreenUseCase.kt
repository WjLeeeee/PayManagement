package com.woojin.paymanagement.domain.usecase.navigation

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.model.NavigationState
import com.woojin.paymanagement.domain.model.Screen
import com.woojin.paymanagement.utils.PayPeriod
import kotlinx.datetime.LocalDate

/**
 * 화면 네비게이션을 처리하는 UseCase
 * SRP: 네비게이션 로직만 담당
 */
class NavigateToScreenUseCase {

    /**
     * 급여일 설정 완료 후 캘린더 화면으로 이동
     */
    fun navigateToCalendarFromSetup(
        currentState: NavigationState
    ): NavigationState {
        return currentState.copy(
            currentScreen = Screen.Calendar,
            previousScreen = Screen.PaydaySetup
        )
    }

    /**
     * 날짜 상세 화면으로 이동
     */
    fun navigateToDateDetail(
        currentState: NavigationState,
        selectedDate: LocalDate
    ): NavigationState {
        return currentState.copy(
            currentScreen = Screen.DateDetail,
            previousScreen = Screen.Calendar,
            selectedDate = selectedDate
        )
    }

    /**
     * 통계 화면으로 이동
     */
    fun navigateToStatistics(
        currentState: NavigationState,
        payPeriod: PayPeriod
    ): NavigationState {
        return currentState.copy(
            currentScreen = Screen.Statistics,
            previousScreen = Screen.Calendar,
            selectedPayPeriod = payPeriod
        )
    }

    /**
     * 거래 추가 화면으로 이동
     */
    fun navigateToAddTransaction(
        currentState: NavigationState,
        editTransaction: Transaction? = null,
        previousScreen: Screen = Screen.Calendar
    ): NavigationState {
        return currentState.copy(
            currentScreen = Screen.AddTransaction,
            previousScreen = previousScreen,
            editTransaction = editTransaction
        )
    }

    /**
     * 이전 화면으로 돌아가기
     */
    fun navigateBack(
        currentState: NavigationState
    ): NavigationState {
        return currentState.copy(
            currentScreen = currentState.previousScreen,
            editTransaction = null
        )
    }

    /**
     * 캘린더 화면으로 돌아가기
     */
    fun navigateToCalendar(
        currentState: NavigationState
    ): NavigationState {
        return currentState.copy(
            currentScreen = Screen.Calendar,
            editTransaction = null
        )
    }

    /**
     * 급여 기간 변경
     */
    fun updateCalendarPayPeriod(
        currentState: NavigationState,
        payPeriod: PayPeriod
    ): NavigationState {
        return currentState.copy(
            currentCalendarPayPeriod = payPeriod
        )
    }
}