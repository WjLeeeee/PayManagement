package com.woojin.paymanagement.data

import androidx.compose.ui.graphics.Color
import com.woojin.paymanagement.theme.CategoryColors

data class ChartItem(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

data class ChartData(
    val incomeItems: List<ChartItem>,
    val expenseItems: List<ChartItem>,
    val investmentItems: List<ChartItem>,
    val savingItems: List<ChartItem>,
    val totalIncome: Double,
    val totalExpense: Double,
    val totalInvestment: Double,
    val totalSaving: Double
)

object ChartDataCalculator {

    fun calculateChartData(transactions: List<Transaction>): ChartData {
        val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        val investmentTransactions = transactions.filter { it.type == TransactionType.INVESTMENT }
        val savingTransactions = transactions.filter { it.type == TransactionType.SAVING }

        val totalIncome = incomeTransactions.sumOf { it.displayAmount }
        val totalExpense = expenseTransactions.sumOf { it.displayAmount }
        val totalInvestment = investmentTransactions.sumOf { it.displayAmount }
        val totalSaving = savingTransactions.sumOf { it.displayAmount }

        val incomeItems = calculateChartItems(incomeTransactions, totalIncome)
        val expenseItems = calculateChartItems(expenseTransactions, totalExpense)
        val investmentItems = calculateInvestmentChartItems(investmentTransactions)
        val savingItems = calculateChartItems(savingTransactions, totalSaving)

        return ChartData(
            incomeItems = incomeItems,
            expenseItems = expenseItems,
            investmentItems = investmentItems,
            savingItems = savingItems,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalInvestment = totalInvestment,
            totalSaving = totalSaving
        )
    }
    
    private fun calculateChartItems(transactions: List<Transaction>, total: Double): List<ChartItem> {
        if (total == 0.0) return emptyList()

        val groupedByCategory = transactions.groupBy { it.category }
        val items = mutableListOf<ChartItem>()
        var colorIndex = 0

        groupedByCategory.forEach { (category, categoryTransactions) ->
            val amount = categoryTransactions.sumOf { it.displayAmount }
            val percentage = (amount / total * 100).toFloat()

            val color = CategoryColors.getColor(category, colorIndex)
            colorIndex++

            items.add(
                ChartItem(
                    category = category,
                    amount = amount,
                    percentage = percentage,
                    color = color
                )
            )
        }

        // 퍼센티지 기준으로 내림차순 정렬
        return items.sortedByDescending { it.percentage }
    }

    private fun calculateInvestmentChartItems(transactions: List<Transaction>): List<ChartItem> {
        if (transactions.isEmpty()) return emptyList()

        // 전체 투자 활동 금액의 절대값 합계 계산
        val totalAbsoluteAmount = transactions.sumOf { kotlin.math.abs(it.displayAmount) }
        if (totalAbsoluteAmount == 0.0) return emptyList()

        val groupedByCategory = transactions.groupBy { it.category }
        val items = mutableListOf<ChartItem>()
        var colorIndex = 0

        groupedByCategory.forEach { (category, categoryTransactions) ->
            val amount = categoryTransactions.sumOf { it.displayAmount }
            val absoluteAmount = kotlin.math.abs(amount)
            val percentage = (absoluteAmount / totalAbsoluteAmount * 100).toFloat()

            val color = CategoryColors.getColor(category, colorIndex)
            colorIndex++

            items.add(
                ChartItem(
                    category = category,
                    amount = amount,
                    percentage = percentage,
                    color = color
                )
            )
        }

        // 퍼센티지 기준으로 내림차순 정렬
        return items.sortedByDescending { it.percentage }
    }
}