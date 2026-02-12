package com.woojin.paymanagement.presentation.cardmanagement

import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.CustomPaymentMethod
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.Transaction

enum class CardTab {
    CARD_MANAGEMENT,
    BALANCE_GIFT
}

data class CardManagementUiState(
    val selectedTab: CardTab = CardTab.CARD_MANAGEMENT,
    val balanceCards: List<BalanceCard> = emptyList(),
    val giftCards: List<GiftCard> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val expandedCardId: String? = null,
    val cardTransactions: Map<String, List<Transaction>> = emptyMap(),
    val isInitialExpansionDone: Boolean = false,
    // 커스텀 결제수단 관련
    val customPaymentMethods: List<CustomPaymentMethod> = emptyList(),
    val isAddDialogVisible: Boolean = false,
    val newMethodName: String = "",
    val isEditDialogVisible: Boolean = false,
    val editingMethod: CustomPaymentMethod? = null,
    val editMethodName: String = "",
    val showConfirmDialog: Boolean = false,
    val confirmDialogMessage: String = "",
    val pendingUpdate: (() -> Unit)? = null,
    val isDeleteDialogVisible: Boolean = false,
    val deletingMethod: CustomPaymentMethod? = null
)

sealed class CardItem {
    data class Balance(val card: BalanceCard) : CardItem()
    data class Gift(val card: GiftCard) : CardItem()

    fun getId(): String = when (this) {
        is Balance -> card.id
        is Gift -> card.id
    }
}
