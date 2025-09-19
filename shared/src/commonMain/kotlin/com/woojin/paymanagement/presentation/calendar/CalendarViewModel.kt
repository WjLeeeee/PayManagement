package com.woojin.paymanagement.presentation.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.repository.PreferencesRepository
import com.woojin.paymanagement.domain.usecase.GetDailyTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.GetPayPeriodSummaryUseCase
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.PayPeriodCalculator
import kotlinx.datetime.LocalDate

class CalendarViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val getPayPeriodSummaryUseCase: GetPayPeriodSummaryUseCase,
    private val getDailyTransactionsUseCase: GetDailyTransactionsUseCase
) {
    var uiState by mutableStateOf(CalendarUiState())
        private set

    private val payday: Int get() = preferencesRepository.getPayday()
    private val adjustment: com.woojin.paymanagement.utils.PaydayAdjustment get() = preferencesRepository.getPaydayAdjustment()

    fun initializeCalendar(
        transactions: List<Transaction>,
        initialPayPeriod: PayPeriod? = null,
        selectedDate: LocalDate? = null
    ) {
        val currentPayPeriod = initialPayPeriod
            ?: PayPeriodCalculator.getCurrentPayPeriod(payday, adjustment)

        val recommendedDate = selectedDate
            ?: PayPeriodCalculator.getRecommendedDateForPeriod(currentPayPeriod, payday, adjustment)

        updateState(
            transactions = transactions,
            payPeriod = currentPayPeriod,
            selectedDate = recommendedDate
        )
    }

    fun updateTransactions(transactions: List<Transaction>) {
        updateState(transactions = transactions)
    }

    fun selectDate(date: LocalDate) {
        updateState(selectedDate = date)
    }

    fun navigateToPreviousPeriod() {
        val previousPeriod = PayPeriodCalculator.getPreviousPayPeriod(
            currentPeriod = requireNotNull(uiState.currentPayPeriod),
            payday = payday,
            adjustment = adjustment
        )

        val newSelectedDate = PayPeriodCalculator.getRecommendedDateForPeriod(
            payPeriod = previousPeriod,
            payday = payday,
            adjustment = adjustment
        )

        updateState(
            payPeriod = previousPeriod,
            selectedDate = newSelectedDate
        )
    }

    fun navigateToNextPeriod() {
        val nextPeriod = PayPeriodCalculator.getNextPayPeriod(
            currentPeriod = requireNotNull(uiState.currentPayPeriod),
            payday = payday,
            adjustment = adjustment
        )

        val newSelectedDate = PayPeriodCalculator.getRecommendedDateForPeriod(
            payPeriod = nextPeriod,
            payday = payday,
            adjustment = adjustment
        )

        updateState(
            payPeriod = nextPeriod,
            selectedDate = newSelectedDate
        )
    }

    private fun updateState(
        transactions: List<Transaction> = uiState.transactions,
        payPeriod: PayPeriod = uiState.currentPayPeriod ?: PayPeriodCalculator.getCurrentPayPeriod(
            payday,
            adjustment
        ),
        selectedDate: LocalDate = uiState.selectedDate
            ?: PayPeriodCalculator.getRecommendedDateForPeriod(payPeriod, payday, adjustment)
    ) {
        val payPeriodSummary = getPayPeriodSummaryUseCase(transactions, payPeriod)
        val dailyTransactions = getDailyTransactionsUseCase(transactions, selectedDate)

        uiState = uiState.copy(
            currentPayPeriod = payPeriod,
            selectedDate = selectedDate,
            transactions = transactions,
            payPeriodSummary = payPeriodSummary,
            dailyTransactions = dailyTransactions
        )
    }
}