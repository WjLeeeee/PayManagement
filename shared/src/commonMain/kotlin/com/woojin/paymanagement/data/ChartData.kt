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
    val totalIncome: Double,
    val totalExpense: Double
)

object ChartDataCalculator {
    
    fun calculateChartData(transactions: List<Transaction>): ChartData {
        val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        
        val totalIncome = incomeTransactions.sumOf { it.amount }
        val totalExpense = expenseTransactions.sumOf { it.amount }
        
        val incomeItems = calculateChartItems(incomeTransactions, totalIncome)
        val expenseItems = calculateChartItems(expenseTransactions, totalExpense)
        
        return ChartData(
            incomeItems = incomeItems,
            expenseItems = expenseItems,
            totalIncome = totalIncome,
            totalExpense = totalExpense
        )
    }
    
    private fun calculateChartItems(transactions: List<Transaction>, total: Double): List<ChartItem> {
        if (total == 0.0) return emptyList()
        
        val groupedByCategory = transactions.groupBy { it.category }
        val items = mutableListOf<ChartItem>()
        var colorIndex = 0
        
        groupedByCategory.forEach { (category, categoryTransactions) ->
            val amount = categoryTransactions.sumOf { it.amount }
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
}