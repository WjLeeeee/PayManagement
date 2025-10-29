package com.woojin.paymanagement.data

import kotlinx.datetime.LocalDate

/**
 * 급여 사이클별 예산 계획
 */
data class BudgetPlan(
    val id: String,
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
    val createdAt: LocalDate
)

/**
 * 카테고리별 예산 배분
 * - 단일 카테고리: categoryIds에 1개의 ID만 포함
 * - 카테고리 그룹: categoryIds에 여러 개의 ID 포함, categoryName과 categoryEmoji는 그룹명과 그룹 이모지
 */
data class CategoryBudget(
    val id: String,
    val budgetPlanId: String,
    val categoryIds: List<String>,  // 단일 또는 복수 카테고리 ID
    val categoryName: String,        // 단일: 카테고리명, 그룹: 그룹명
    val categoryEmoji: String,       // 단일: 카테고리 이모지, 그룹: 그룹 이모지
    val allocatedAmount: Double
) {
    // 편의 속성
    val isGroup: Boolean get() = categoryIds.size > 1
    val categoryId: String get() = categoryIds.firstOrNull() ?: ""  // 하위 호환성
}
