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
    val deletingMethod: CustomPaymentMethod? = null,
    // 잔액권 추가
    val isAddBalanceCardDialogVisible: Boolean = false,
    val newBalanceCardName: String = "",
    val newBalanceCardAmount: String = "",
    // 잔액권 수정/삭제
    val isEditBalanceCardDialogVisible: Boolean = false,
    val editingBalanceCard: BalanceCard? = null,
    val editBalanceCardName: String = "",
    val editBalanceCardCurrentBalance: String = "",
    val isDeleteBalanceCardDialogVisible: Boolean = false,
    val deletingBalanceCard: BalanceCard? = null,
    // 상품권 추가
    val isAddGiftCardDialogVisible: Boolean = false,
    val newGiftCardName: String = "",
    val newGiftCardAmount: String = "",
    // 상품권 수정/삭제
    val isEditGiftCardDialogVisible: Boolean = false,
    val editingGiftCard: GiftCard? = null,
    val editGiftCardName: String = "",
    val editGiftCardRemainingAmount: String = "",
    val isDeleteGiftCardDialogVisible: Boolean = false,
    val deletingGiftCard: GiftCard? = null
)

sealed class CardItem {
    data class Balance(val card: BalanceCard) : CardItem()
    data class Gift(val card: GiftCard) : CardItem()

    fun getId(): String = when (this) {
        is Balance -> card.id
        is Gift -> card.id
    }
}
