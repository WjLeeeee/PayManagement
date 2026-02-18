package com.woojin.paymanagement.presentation.statistics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.repository.PreferencesRepository
import com.woojin.paymanagement.domain.usecase.AnalyzePaymentMethodsUseCase
import com.woojin.paymanagement.domain.usecase.CalculateChartDataUseCase
import com.woojin.paymanagement.domain.usecase.GetPayPeriodTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.GetCategoriesUseCase
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.PayPeriodCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val getPayPeriodTransactionsUseCase: GetPayPeriodTransactionsUseCase,
    private val calculateChartDataUseCase: CalculateChartDataUseCase,
    private val analyzePaymentMethodsUseCase: AnalyzePaymentMethodsUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val payPeriodCalculator: PayPeriodCalculator,
    private val coroutineScope: CoroutineScope
) {
    var uiState by mutableStateOf(StatisticsUiState())
        private set

    init {
        // 카테고리 목록을 로드하여 UiState에 반영
        coroutineScope.launch {
            combine(
                getCategoriesUseCase(TransactionType.INCOME),
                getCategoriesUseCase(TransactionType.EXPENSE),
                getCategoriesUseCase(TransactionType.SAVING)
            ) { income, expense, saving ->
                income + expense + saving
            }.collect { categories ->
                uiState = uiState.copy(availableCategories = categories)
            }
        }
    }

    fun initializeStatistics(
        initialPayPeriod: PayPeriod?,
        availableBalanceCards: List<BalanceCard>,
        availableGiftCards: List<GiftCard>
    ) {
        coroutineScope.launch {
            val payday = preferencesRepository.getPayday()
            val adjustment = preferencesRepository.getPaydayAdjustment()

            val currentPayPeriod = initialPayPeriod ?: payPeriodCalculator.getCurrentPayPeriod(payday, adjustment)

            uiState = uiState.copy(currentPayPeriod = currentPayPeriod)

            loadStatisticsData(availableBalanceCards, availableGiftCards)
        }
    }

    fun getStatisticsFlow(
        availableBalanceCards: List<BalanceCard>,
        availableGiftCards: List<GiftCard>
    ): Flow<StatisticsUiState> {
        val currentPayPeriod = uiState.currentPayPeriod ?: return kotlinx.coroutines.flow.flowOf(uiState)

        return getPayPeriodTransactionsUseCase(currentPayPeriod)
            .combine(kotlinx.coroutines.flow.flowOf(availableBalanceCards)) { transactions, balanceCards ->
                val chartData = calculateChartDataUseCase(transactions)
                val paymentSummary = analyzePaymentMethodsUseCase(transactions, balanceCards, availableGiftCards)

                uiState.copy(
                    transactions = transactions,
                    chartData = chartData,
                    paymentSummary = paymentSummary,
                    isLoading = false
                )
            }
    }

    fun moveToPreviousPeriod() {
        val currentPeriod = uiState.currentPayPeriod ?: return
        val payday = preferencesRepository.getPayday()
        val adjustment = preferencesRepository.getPaydayAdjustment()

        coroutineScope.launch {
            val previousPeriod = payPeriodCalculator.getPreviousPayPeriod(currentPeriod, payday, adjustment)
            uiState = uiState.copy(currentPayPeriod = previousPeriod)
        }
    }

    fun moveToNextPeriod() {
        val currentPeriod = uiState.currentPayPeriod ?: return
        val payday = preferencesRepository.getPayday()
        val adjustment = preferencesRepository.getPaydayAdjustment()

        coroutineScope.launch {
            val nextPeriod = payPeriodCalculator.getNextPayPeriod(currentPeriod, payday, adjustment)
            uiState = uiState.copy(currentPayPeriod = nextPeriod)
        }
    }

    fun showCalculatorDialog() {
        uiState = uiState.copy(showCalculatorDialog = true)
    }

    fun hideCalculatorDialog() {
        uiState = uiState.copy(showCalculatorDialog = false)
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    private fun loadStatisticsData(
        availableBalanceCards: List<BalanceCard>,
        availableGiftCards: List<GiftCard>
    ) {
        uiState = uiState.copy(isLoading = true)
        // Flow는 외부에서 수집되므로 여기서는 로딩 상태만 설정
    }
}