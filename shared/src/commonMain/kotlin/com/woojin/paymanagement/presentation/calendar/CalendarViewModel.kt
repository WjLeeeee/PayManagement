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
import kotlinx.datetime.LocalDate

class CalendarViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val getPayPeriodSummaryUseCase: GetPayPeriodSummaryUseCase,
    private val getDailyTransactionsUseCase: GetDailyTransactionsUseCase,
    private val getMoneyVisibilityUseCase: GetMoneyVisibilityUseCase,
    private val setMoneyVisibilityUseCase: SetMoneyVisibilityUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val coroutineScope: CoroutineScope
) {
    var uiState by mutableStateOf(CalendarUiState())
        private set

    private val payday: Int get() = preferencesRepository.getPayday()
    private val adjustment: com.woojin.paymanagement.utils.PaydayAdjustment get() = preferencesRepository.getPaydayAdjustment()

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
        val currentPayPeriod = initialPayPeriod
            ?: PayPeriodCalculator.getCurrentPayPeriod(payday, adjustment)

        val recommendedDate = selectedDate
            ?: PayPeriodCalculator.getRecommendedDateForPeriod(currentPayPeriod, payday, adjustment)

        val isMoneyVisible = getMoneyVisibilityUseCase()

        updateState(
            transactions = transactions,
            payPeriod = currentPayPeriod,
            selectedDate = recommendedDate,
            isMoneyVisible = isMoneyVisible
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
        payPeriod: PayPeriod = uiState.currentPayPeriod ?: PayPeriodCalculator.getCurrentPayPeriod(
            payday,
            adjustment
        ),
        selectedDate: LocalDate = uiState.selectedDate
            ?: PayPeriodCalculator.getRecommendedDateForPeriod(payPeriod, payday, adjustment),
        isMoneyVisible: Boolean = uiState.isMoneyVisible
    ) {
        val payPeriodSummary = getPayPeriodSummaryUseCase(transactions, payPeriod)
        val dailyTransactions = getDailyTransactionsUseCase(transactions, selectedDate)

        uiState = uiState.copy(
            currentPayPeriod = payPeriod,
            selectedDate = selectedDate,
            transactions = transactions,
            payPeriodSummary = payPeriodSummary,
            dailyTransactions = dailyTransactions,
            isMoneyVisible = isMoneyVisible
        )
    }
}