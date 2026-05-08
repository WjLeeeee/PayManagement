package com.woojin.paymanagement.presentation.addtransaction

import androidx.compose.ui.text.input.TextFieldValue
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.CustomPaymentMethod
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.Category
import kotlinx.datetime.LocalDate

data class AddTransactionUiState(
    val amount: TextFieldValue = TextFieldValue(""),
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedIncomeType: IncomeType = IncomeType.CASH,
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val selectedBalanceCard: BalanceCard? = null,
    val selectedGiftCard: GiftCard? = null,
    val cardName: String = "",
    val category: String = "",
    val merchant: String = "", // 사용처 (지출일 때만 필수)
    val memo: String = "",
    val date: LocalDate? = null,

    // 더치페이 관련
    val isSettlement: Boolean = false,
    val settlementAmount: String = "",

    // 카드 목록
    val availableBalanceCards: List<BalanceCard> = emptyList(),
    val availableGiftCards: List<GiftCard> = emptyList(),

    // 카테고리 목록 (동적으로 로드)
    val availableCategories: List<Category> = emptyList(),

    // 잔액권 충전 관련 (수입 시)
    val isChargingExistingBalanceCard: Boolean = false,
    val selectedBalanceCardForCharge: BalanceCard? = null,
    val purchaseAmount: String = "",

    // 편집 모드
    val isEditMode: Boolean = false,
    val editTransaction: Transaction? = null,

    // UI 상태
    val isLoading: Boolean = false,
    val error: String? = null,
    val isValidInput: Boolean = false,
    val saveEnabled: Boolean = false,

    // 커스텀 결제수단 (카드)
    val customPaymentMethods: List<CustomPaymentMethod> = emptyList(),
    val selectedCustomCardName: String? = null,

    // 예산 초과 알림
    val budgetExceededMessage: String? = null
)

val AddTransactionUiState.categories: List<String>
    get() = if (availableCategories.isNotEmpty()) {
        // 데이터베이스에서 로드된 카테고리 사용
        availableCategories.map { it.name }
    } else {
        // 기본값 (데이터베이스가 아직 로드되지 않았을 때)
        when (selectedType) {
            TransactionType.INCOME -> listOf("급여", "식비", "기타수입")
            TransactionType.EXPENSE -> listOf("식비", "생활비", "생활용품", "쇼핑", "문화생활", "교통비", "기타지출")
            TransactionType.SAVING -> listOf("적금", "예금")
            TransactionType.INVESTMENT -> listOf("투자", "익절", "손절", "배당금")
        }
    }

// 카테고리별 이모지 매핑 (UiState에서 찾거나 기본값 사용)
fun getCategoryEmoji(category: String, uiState: AddTransactionUiState? = null): String {
    // UiState에서 카테고리를 찾아서 이모지 반환
    uiState?.availableCategories?.find { it.name == category }?.let {
        return it.emoji
    }

    // 기본값 (하드코딩)
    return when (category) {
        // 수입 카테고리
        "급여" -> "💰"
        "식비" -> "🍔"
        "당근" -> "🥕"
        "K-패스 환급" -> "🚌"
        "투자수익" -> "📈"
        "기타수입" -> "💵"

        // 지출 카테고리
        "데이트" -> "💑"
        "생활비" -> "🏠"
        "생활용품" -> "🧴"
        "쇼핑" -> "🛍️"
        "문화생활" -> "🎬"
        "경조사" -> "🎁"
        "자기계발" -> "📚"
        "공과금" -> "💡"
        "대출이자" -> "🏦"
        "모임통장" -> "👥"
        "교통비" -> "🚗"
        "적금" -> "🐷"
        "투자" -> "💹"
        "손절" -> "📉"
        "정기결제" -> "📅"
        "기타지출" -> "💸"

        else -> "📌"
    }
}

// 카테고리 리스트에서 이모지 찾기 (다른 UiState에서도 사용 가능)
fun getCategoryEmoji(category: String, availableCategories: List<Category>): String {
    // 카테고리 리스트에서 찾아서 이모지 반환
    availableCategories.find { it.name == category }?.let {
        return it.emoji
    }

    // 못 찾으면 기본값 사용
    return getCategoryEmoji(category, null)
}