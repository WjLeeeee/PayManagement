package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.presentation.calculator.CalculatorRequest
import com.woojin.paymanagement.presentation.calculator.CalculatorResult
import com.woojin.paymanagement.presentation.calculator.CategorySummary
import com.woojin.paymanagement.presentation.calculator.TransactionDetail
import kotlinx.datetime.LocalDate

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

        // 거래 상세 정보 (날짜순 정렬)
        val transactionDetails = filteredTransactions
            .sortedBy { it.date }
            .map { transaction ->
                TransactionDetail(
                    amount = transaction.amount,
                    memo = transaction.memo,
                    date = transaction.date
                )
            }

        return CalculatorResult(
            totalAmount = totalAmount,
            transactionCount = transactionCount,
            averageAmount = averageAmount,
            categories = categorySummaries,
            transactionDetails = transactionDetails
        )
    }

    fun getAvailableCategories(
        transactions: List<Transaction>,
        startDate: LocalDate,
        endDate: LocalDate,
        transactionType: TransactionType? = null
    ): List<String> {
        // 기간으로 필터링
        val periodTransactions = transactions.filter { transaction ->
            transaction.date >= startDate && transaction.date <= endDate
        }

        // 거래 타입으로 필터링
        val filteredTransactions = if (transactionType != null) {
            periodTransactions.filter { it.type == transactionType }
        } else {
            periodTransactions
        }

        // 거래 내역이 1건 이상 있는 카테고리만 반환
        return filteredTransactions
            .map { it.category }
            .distinct()
            .sorted()
    }
}