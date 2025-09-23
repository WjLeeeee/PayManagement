package com.woojin.paymanagement.presentation.addtransaction

import androidx.compose.ui.text.input.TextFieldValue
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
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
    val memo: String = "",
    val date: LocalDate? = null,

    // 더치페이 관련
    val isSettlement: Boolean = false,
    val actualAmount: String = "",
    val splitCount: String = "",
    val settlementAmount: String = "",

    // 카드 목록
    val availableBalanceCards: List<BalanceCard> = emptyList(),
    val availableGiftCards: List<GiftCard> = emptyList(),

    // 편집 모드
    val isEditMode: Boolean = false,
    val editTransaction: Transaction? = null,

    // UI 상태
    val isLoading: Boolean = false,
    val error: String? = null,
    val isValidInput: Boolean = false,
    val saveEnabled: Boolean = false
)

val AddTransactionUiState.categories: List<String>
    get() = when (selectedType) {
        TransactionType.INCOME -> listOf("급여", "식비", "중고거래", "K-패스 환급", "투자수익", "기타수입")
        TransactionType.EXPENSE -> listOf("식비", "데이트", "교통비", "생활용품", "쇼핑", "적금", "투자", "정기결제", "기타지출")
    }