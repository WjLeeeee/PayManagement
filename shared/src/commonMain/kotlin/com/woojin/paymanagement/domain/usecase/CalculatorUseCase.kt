package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.presentation.calculator.CalculatorRequest
import com.woojin.paymanagement.presentation.calculator.CalculatorResult
import com.woojin.paymanagement.presentation.calculator.CategorySummary

class CalculatorUseCase {

    fun calculate(
        transactions: List<Transaction>,
        request: CalculatorRequest
    ): CalculatorResult {
        // 기간으로 필터링
        val periodTransactions = transactions.filter { transaction ->
            transaction.date >= request.startDate && transaction.date <= request.endDate
        }

        // 거래 타입으로 필터링 (null이면 모든 타입)
        val typeFilteredTransactions = if (request.transactionType != null) {
            periodTransactions.filter { it.type == request.transactionType }
        } else {
            periodTransactions
        }

        // 카테고리로 필터링 (빈 리스트면 모든 카테고리)
        val filteredTransactions = if (request.categories.isNotEmpty()) {
            typeFilteredTransactions.filter { transaction ->
                request.categories.contains(transaction.category)
            }
        } else {
            typeFilteredTransactions
        }

        // 총액 계산
        val totalAmount = filteredTransactions.sumOf { it.amount }

        // 거래 건수
        val transactionCount = filteredTransactions.size

        // 평균 금액
        val averageAmount = if (transactionCount > 0) totalAmount / transactionCount else 0.0

        // 카테고리별 요약
        val categoryGroups = filteredTransactions.groupBy { it.category }
        val categorySummaries = categoryGroups.map { (category, transactions) ->
            val categoryAmount = transactions.sumOf { it.amount }
            val categoryCount = transactions.size
            val percentage = if (totalAmount > 0) (categoryAmount / totalAmount) * 100 else 0.0

            CategorySummary(
                category = category,
                amount = categoryAmount,
                transactionCount = categoryCount,
                percentage = percentage
            )
        }.sortedByDescending { it.amount }

        return CalculatorResult(
            totalAmount = totalAmount,
            transactionCount = transactionCount,
            averageAmount = averageAmount,
            categories = categorySummaries
        )
    }

    fun getAvailableCategories(
        transactions: List<Transaction>,
        transactionType: TransactionType? = null
    ): List<String> {
        val filteredTransactions = if (transactionType != null) {
            transactions.filter { it.type == transactionType }
        } else {
            transactions
        }

        return filteredTransactions
            .map { it.category }
            .distinct()
            .sorted()
    }
}