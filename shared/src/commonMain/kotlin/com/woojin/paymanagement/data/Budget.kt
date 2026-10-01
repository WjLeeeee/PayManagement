package com.woojin.paymanagement.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * 예산 템플릿
 * effectiveFromDate부터 다음 템플릿 시작 전까지 유효
 */
data class BudgetPlan(
    val id: String,
    val effectiveFromDate: LocalDate,  // 이 날짜부터 적용
    val monthlySalary: Double,          // 고정 급여
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
    val allocatedAmount: Double,
    val memo: String? = null,        // 카테고리 예산에 대한 메모
    val items: List<BudgetItem> = emptyList()  // 세부 항목 (예: 월세 500,000 / 관리비 150,000)
) {
    // 편의 속성
    val isGroup: Boolean get() = categoryIds.size > 1
    val itemsTotal: Double get() = items.sumOf { it.amount }
    val categoryId: String get() = categoryIds.firstOrNull() ?: ""  // 하위 호환성
}

/**
 * 카테고리 예산의 세부 항목
 * allocatedAmount를 어디에 얼마씩 배정했는지 나타내는 내역 (지출 추적 단위는 아님)
 */
@Serializable
data class BudgetItem(
    val id: String,
    val name: String,
    val amount: Double
)
