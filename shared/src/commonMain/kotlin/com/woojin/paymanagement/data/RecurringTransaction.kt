package com.woojin.paymanagement.data

/**
 * 반복 거래 패턴
 */
enum class RecurringPattern {
    MONTHLY,  // 매달 (특정 날짜)
    WEEKLY    // 매주 (특정 요일)
}

/**
 * 반복 거래
 */
data class RecurringTransaction(
    val id: String,
    val type: TransactionType,
    val category: String,
    val amount: Double,
    val merchant: String,  // 사용처 (필수)
    val memo: String = "",  // 메모 (선택)
    val paymentMethod: PaymentMethod,
    val balanceCardId: String? = null,
    val giftCardId: String? = null,
    val pattern: RecurringPattern,
    val dayOfMonth: Int? = null,  // 1~31 (MONTHLY일 때 사용)
    val dayOfWeek: Int? = null,   // 1~7 (월~일, WEEKLY일 때 사용)
    val isActive: Boolean = true,  // 활성화 여부
    val createdAt: Long,
    val lastExecutedDate: String? = null  // "2025-01-15" 마지막으로 자동 추가된 날짜
) {
    /**
     * 오늘 실행해야 하는지 확인
     */
    fun shouldExecuteToday(today: kotlinx.datetime.LocalDate): Boolean {
        if (!isActive) return false

        return when (pattern) {
            RecurringPattern.MONTHLY -> {
                dayOfMonth != null && today.dayOfMonth == dayOfMonth
            }
            RecurringPattern.WEEKLY -> {
                dayOfWeek != null && today.dayOfWeek.value == dayOfWeek
            }
        }
    }

    /**
     * 이미 오늘 실행됐는지 확인
     */
    fun isExecutedToday(today: kotlinx.datetime.LocalDate): Boolean {
        return lastExecutedDate == today.toString()
    }
}
