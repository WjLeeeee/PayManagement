package com.woojin.paymanagement.presentation.cardmanagement

import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.Transaction

enum class CardTab {
    ACTIVE,
    INACTIVE
}

data class CardManagementUiState(
    val selectedTab: CardTab = CardTab.ACTIVE,
    val balanceCards: List<BalanceCard> = emptyList(),
    val giftCards: List<GiftCard> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val expandedCardId: String? = null, // 확장된 카드 ID
    val cardTransactions: Map<String, List<Transaction>> = emptyMap(), // 카드별 거래 내역
    val isInitialExpansionDone: Boolean = false // 초기 확장 완료 여부
)

sealed class CardItem {
    data class Balance(val card: BalanceCard) : CardItem()
    data class Gift(val card: GiftCard) : CardItem()

    fun getId(): String = when (this) {
        is Balance -> card.id
        is Gift -> card.id
    }
}
