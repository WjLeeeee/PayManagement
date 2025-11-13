package com.woojin.paymanagement.presentation.monthlycomparison

import com.woojin.paymanagement.data.Category

data class MonthlyComparisonUiState(
    val currentMonth: String = "",  // "2025년 1월"
    val previousMonth: String = "",  // "2024년 12월"
    val categoryComparisons: List<CategoryComparison> = emptyList(),
    val totalCurrentMonth: Double = 0.0,
    val totalPreviousMonth: Double = 0.0,
    val totalDifference: Double = 0.0,
    val totalDifferencePercentage: Float = 0f,
    val isLoading: Boolean = false,
    val availableCategories: List<Category> = emptyList()
)

data class CategoryComparison(
    val categoryName: String,
    val currentMonthAmount: Double,
    val previousMonthAmount: Double,
    val difference: Double,  // currentMonth - previousMonth (양수면 증가, 음수면 감소)
    val differencePercentage: Float  // 증감률
) {
    val isIncrease: Boolean get() = difference > 0
    val isDecrease: Boolean get() = difference < 0
    val isUnchanged: Boolean get() = difference == 0.0
}
