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

    // 투자 관련 카테고리 목록
    private val INVESTMENT_CATEGORIES = setOf("투자", "손절", "익절", "배당금")

    fun calculateChartData(transactions: List<Transaction>): ChartData {
        // 투자 관련 카테고리 제외
        val incomeTransactions = transactions.filter {
            it.type == TransactionType.INCOME && it.category !in INVESTMENT_CATEGORIES
        }
        val expenseTransactions = transactions.filter {
            it.type == TransactionType.EXPENSE && it.category !in INVESTMENT_CATEGORIES
        }

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