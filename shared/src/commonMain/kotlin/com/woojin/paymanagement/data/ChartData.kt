package com.woojin.paymanagement.data

import androidx.compose.ui.graphics.Color

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
    
    // 미리 정의된 카테고리별 색상
    private val categoryColors = mapOf(
        // 수입 카테고리
        "급여" to Color(0xFF1E88E5),      // 신뢰감 있는 파란색 (직장/안정감)
        "식비" to Color(0xFF4CAF50),      // 신선한 초록색 (음식/건강)
        "당근" to Color(0xFFFF7043),      // 당근색 (당근마켓 브랜드 컬러)
        "K-패스 환급" to Color(0xFF00ACC1), // 시원한 청록색 (교통/환급)
        "투자수익" to Color(0xFF388E3C),   // 진한 초록색 (돈/수익)
        "기타수입" to Color(0xFF9C27B0),   // 보라색 (기타/특별함)

        // 지출 카테고리
        "교통비" to Color(0xFF2196F3),     // 파란색 (교통/이동)
        "생활용품" to Color(0xFF607D8B),   // 회색 (일상용품)
        "쇼핑" to Color(0xFFE91E63),       // 핑크색 (쇼핑/소비)
        "적금" to Color(0xFF43A047),       // 초록색 (저축/미래)
        "투자" to Color(0xFF7B1FA2),       // 진한 보라색 (투자/장기)
        "기타지출" to Color(0xFF795548)    // 갈색 (기타/일반)
    )
    
    // 기본 색상 팔레트 (카테고리에 정의되지 않은 경우 사용)
    private val defaultColors = listOf(
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFF9C27B0),
        Color(0xFF00BCD4), Color(0xFF3F51B5), Color(0xFF009688),
        Color(0xFF607D8B), Color(0xFFFF5722), Color(0xFFE91E63),
        Color(0xFF795548), Color(0xFFFF9800), Color(0xFF8BC34A),
        Color(0xFFFFEB3B), Color(0xFF673AB7), Color(0xFF9E9E9E)
    )
    
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
            
            val color = categoryColors[category] ?: defaultColors[colorIndex % defaultColors.size]
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