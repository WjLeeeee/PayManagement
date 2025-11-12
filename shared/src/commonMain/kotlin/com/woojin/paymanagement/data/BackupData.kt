package com.woojin.paymanagement.data

import kotlinx.serialization.Serializable

/**
 * 백업/복원을 위한 데이터 모델
 */
@Serializable
data class BackupData(
    val version: Int = 4, // 백업 데이터 버전 (v4: budget template system)
    val exportDate: String = "", // 내보내기 날짜
    val payday: Int = 0, // 월급날
    val paydayAdjustment: String = "NONE", // 월급날 조정 (enum name)
    val transactions: List<TransactionBackup> = emptyList(),
    val balanceCards: List<BalanceCardBackup> = emptyList(),
    val giftCards: List<GiftCardBackup> = emptyList(),
    val categories: List<CategoryBackup> = emptyList(), // v3부터 추가
    val budgetPlans: List<BudgetPlanBackup> = emptyList(), // v3부터 추가
    val categoryBudgets: List<CategoryBudgetBackup> = emptyList() // v3부터 추가
)

@Serializable
data class TransactionBackup(
    val id: String,
    val date: String, // ISO 8601 형식 (YYYY-MM-DD)
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String?,
    val memo: String?,
    val paymentMethod: String?,
    val incomeType: String?,
    val balanceCardId: String?,
    val giftCardId: String?,
    val cardName: String?,
    val merchant: String? = null, // 사용처 (v2부터 추가)
    val actualAmount: Double? = null, // 실제 결제액 - 더치페이 (v2부터 추가)
    val settlementAmount: Double? = null, // 정산받은 금액 - 더치페이 (v2부터 추가)
    val isSettlement: Boolean = false // 더치페이 여부 (v2부터 추가)
)

@Serializable
data class BalanceCardBackup(
    val id: String,
    val name: String,
    val initialAmount: Double,
    val currentBalance: Double,
    val createdDate: String, // ISO 8601 형식
    val isActive: Boolean
)

@Serializable
data class GiftCardBackup(
    val id: String,
    val name: String,
    val totalAmount: Double,
    val usedAmount: Double,
    val createdDate: String, // ISO 8601 형식
    val isActive: Boolean,
    val minimumUsageRate: Double = 0.8 // v2부터 추가
)

@Serializable
data class CategoryBackup(
    val id: String,
    val name: String,
    val emoji: String,
    val type: String, // "INCOME" or "EXPENSE"
    val isActive: Boolean,
    val sortOrder: Int
)

@Serializable
data class BudgetPlanBackup(
    val id: String,
    val effectiveFromDate: String = "", // ISO 8601 형식 (YYYY-MM-DD) - v4부터, v3 호환을 위해 기본값
    val monthlySalary: Double = 0.0, // v4부터, v3 호환을 위해 기본값
    val createdAt: String, // ISO 8601 형식
    // v3 하위 호환성을 위한 필드
    val periodStartDate: String? = null,
    val periodEndDate: String? = null
)

@Serializable
data class CategoryBudgetBackup(
    val id: String,
    val budgetPlanId: String,
    val categoryIds: List<String>, // JSON 배열
    val categoryName: String,
    val categoryEmoji: String,
    val allocatedAmount: Double,
    val memo: String? = null // v3부터 추가
)
