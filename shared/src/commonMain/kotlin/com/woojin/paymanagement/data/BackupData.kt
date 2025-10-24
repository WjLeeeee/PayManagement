package com.woojin.paymanagement.data

import kotlinx.serialization.Serializable

/**
 * 백업/복원을 위한 데이터 모델
 */
@Serializable
data class BackupData(
    val version: Int = 1, // 백업 데이터 버전
    val exportDate: String, // 내보내기 날짜
    val payday: Int, // 월급날
    val paydayAdjustment: String, // 월급날 조정 (enum name)
    val transactions: List<TransactionBackup>,
    val balanceCards: List<BalanceCardBackup>,
    val giftCards: List<GiftCardBackup>
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
    val cardName: String?
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
    val isActive: Boolean
)
