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
    val createdAt: LocalDate,
    val transfers: List<TransferItem> = emptyList()  // 월급날 이체 계획 (통장별 이체 금액)
) {
    val transfersTotal: Double get() = transfers.sumOf { it.amount }
}

/**
 * 월급날 이체 계획의 한 줄: 어느 통장에 얼마를 넣을지
 * id는 급여 변경으로 템플릿이 재생성돼도 유지되어 CategoryBudget.accountAllocations 참조가 끊기지 않음
 */
@Serializable
data class TransferItem(
    val id: String,
    val accountName: String,
    val amount: Double
)

/**
 * 특정 급여 기간에 이체를 완료했다고 체크한 기록
 */
data class TransferCheck(
    val periodStartDate: LocalDate,
    val transferItemId: String
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
    val items: List<BudgetItem> = emptyList(),  // 세부 항목 (예: 월세 500,000 / 관리비 150,000)
    val accountAllocations: List<BudgetAccountAllocation> = emptyList()  // 이 예산을 사용할 통장과 통장별 금액, 선택사항
) {
    // 편의 속성
    val isGroup: Boolean get() = categoryIds.size > 1
    val itemsTotal: Double get() = items.sumOf { it.amount }
    val transferItemIds: List<String> get() = accountAllocations.map { it.transferItemId }

    /** 이 예산 중 해당 통장에 배분된 금액. 통장이 하나면 예산 전액, 여럿이면 입력한 금액(없으면 0) */
    fun amountForAccount(transferItemId: String): Double {
        val allocation = accountAllocations.find { it.transferItemId == transferItemId } ?: return 0.0
        return if (accountAllocations.size == 1) allocatedAmount else allocation.amount ?: 0.0
    }
    val categoryId: String get() = categoryIds.firstOrNull() ?: ""  // 하위 호환성
}

/**
 * 예산을 사용할 통장 하나와 그 통장에 배분된 금액
 * amount가 null이면 예산 전액 (통장을 하나만 고른 경우)
 */
@Serializable
data class BudgetAccountAllocation(
    val transferItemId: String,
    val amount: Double? = null
)

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
