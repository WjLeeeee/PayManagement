package com.woojin.paymanagement.presentation.monthlycomparison

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.CategoryRepository
import com.woojin.paymanagement.domain.repository.PreferencesRepository
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.PayPeriodCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class MonthlyComparisonViewModel(
    private val databaseHelper: DatabaseHelper,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val payPeriodCalculator: PayPeriodCalculator,
    private val coroutineScope: CoroutineScope
) {
    var uiState by mutableStateOf(MonthlyComparisonUiState())
        private set

    private var currentPeriod: PayPeriod? = null
    private var previousPeriod: PayPeriod? = null

    init {
        loadData()
    }

    private fun loadData() {
        coroutineScope.launch {
            uiState = uiState.copy(isLoading = true)

            val payday = preferencesRepository.getPayday()
            val adjustment = preferencesRepository.getPaydayAdjustment()
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // 현재 급여 기간 계산
            val currentPayPeriod = payPeriodCalculator.getCurrentPayPeriod(
                currentDate = today,
                payday = payday,
                adjustment = adjustment
            )

            // 이전 급여 기간 계산
            val previousPayPeriod = payPeriodCalculator.getPreviousPayPeriod(
                currentPeriod = currentPayPeriod,
                payday = payday,
                adjustment = adjustment
            )

            currentPeriod = currentPayPeriod
            previousPeriod = previousPayPeriod

            // 카테고리 정보 로드
            val categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()

            // 거래 데이터 로드
            val transactions = databaseHelper.getAllTransactions().firstOrNull() ?: emptyList()

            // 현재 급여 기간 거래 필터링
            val currentPeriodTransactions = transactions.filter { transaction ->
                transaction.date >= currentPayPeriod.startDate && transaction.date <= currentPayPeriod.endDate
            }

            // 이전 급여 기간 거래 필터링
            val previousPeriodTransactions = transactions.filter { transaction ->
                transaction.date >= previousPayPeriod.startDate && transaction.date <= previousPayPeriod.endDate
            }

            // 카테고리별 집계
            val categoryComparisons = calculateCategoryComparisons(
                currentPeriodTransactions = currentPeriodTransactions.filter { it.type == TransactionType.EXPENSE },
                previousPeriodTransactions = previousPeriodTransactions.filter { it.type == TransactionType.EXPENSE }
            )

            // 총 지출 계산
            val totalCurrent = currentPeriodTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            val totalPrevious = previousPeriodTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            val totalDiff = totalCurrent - totalPrevious
            val totalDiffPercentage = if (totalPrevious > 0) {
                ((totalDiff / totalPrevious) * 100).toFloat()
            } else if (totalCurrent > 0) {
                100f
            } else {
                0f
            }

            uiState = uiState.copy(
                currentMonth = currentPayPeriod.displayText,
                previousMonth = previousPayPeriod.displayText,
                categoryComparisons = categoryComparisons,
                totalCurrentMonth = totalCurrent,
                totalPreviousMonth = totalPrevious,
                totalDifference = totalDiff,
                totalDifferencePercentage = totalDiffPercentage,
                isLoading = false,
                availableCategories = categories
            )
        }
    }

    private fun calculateCategoryComparisons(
        currentPeriodTransactions: List<com.woojin.paymanagement.data.Transaction>,
        previousPeriodTransactions: List<com.woojin.paymanagement.data.Transaction>
    ): List<CategoryComparison> {
        // 카테고리별로 그룹화
        val currentByCategory = currentPeriodTransactions.groupBy { it.category }
        val previousByCategory = previousPeriodTransactions.groupBy { it.category }

        // 모든 카테고리 수집 (둘 중 하나라도 있으면 표시)
        val allCategories = (currentByCategory.keys + previousByCategory.keys).distinct()

        return allCategories.map { category ->
            val currentAmount = currentByCategory[category]?.sumOf { it.amount } ?: 0.0
            val previousAmount = previousByCategory[category]?.sumOf { it.amount } ?: 0.0
            val difference = currentAmount - previousAmount
            val differencePercentage = if (previousAmount > 0) {
                ((difference / previousAmount) * 100).toFloat()
            } else if (currentAmount > 0) {
                100f
            } else {
                0f
            }

            CategoryComparison(
                categoryName = category,
                currentMonthAmount = currentAmount,
                previousMonthAmount = previousAmount,
                difference = difference,
                differencePercentage = differencePercentage
            )
        }.sortedByDescending { it.currentMonthAmount } // 현재 기간 지출 기준으로 정렬
    }

    fun moveToPreviousPeriod() {
        val payday = preferencesRepository.getPayday()
        val adjustment = preferencesRepository.getPaydayAdjustment()

        currentPeriod?.let { current ->
            coroutineScope.launch {
                // 현재를 이전으로
                currentPeriod = previousPeriod

                // 새로운 이전 기간 계산
                previousPeriod = payPeriodCalculator.getPreviousPayPeriod(
                    currentPeriod = current,
                    payday = payday,
                    adjustment = adjustment
                )

                loadData()
            }
        }
    }

    fun moveToNextPeriod() {
        val payday = preferencesRepository.getPayday()
        val adjustment = preferencesRepository.getPaydayAdjustment()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        currentPeriod?.let { current ->
            coroutineScope.launch {
                // 다음 급여 기간 계산
                val nextPeriod = payPeriodCalculator.getNextPayPeriod(
                    currentPeriod = current,
                    payday = payday,
                    adjustment = adjustment
                )

                // 미래 급여 기간으로는 이동 불가
                if (nextPeriod.startDate <= today) {
                    previousPeriod = currentPeriod
                    currentPeriod = nextPeriod
                    loadData()
                }
            }
        }
    }

    fun canNavigateNext(): Boolean {
        val payday = preferencesRepository.getPayday()
        val adjustment = preferencesRepository.getPaydayAdjustment()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // This needs to be calculated asynchronously, but for now we'll use a blocking approach
        // In a real scenario, you'd want to make this a suspend function or use a different pattern
        var canNavigate = false
        currentPeriod?.let { current ->
            coroutineScope.launch {
                val nextPeriod = payPeriodCalculator.getNextPayPeriod(
                    currentPeriod = current,
                    payday = payday,
                    adjustment = adjustment
                )
                canNavigate = nextPeriod.startDate <= today
            }
        }
        return canNavigate
    }
}
