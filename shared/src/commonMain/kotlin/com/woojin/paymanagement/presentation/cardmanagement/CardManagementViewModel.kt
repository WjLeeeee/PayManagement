package com.woojin.paymanagement.presentation.cardmanagement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woojin.paymanagement.database.DatabaseHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CardManagementViewModel(
    private val databaseHelper: DatabaseHelper
) : ViewModel() {

    var uiState by mutableStateOf(CardManagementUiState())
        private set

    private var cardsJob: Job? = null

    init {
        loadCards()
    }

    fun selectTab(tab: CardTab) {
        uiState = uiState.copy(
            selectedTab = tab,
            expandedCardId = null // 탭 전환 시 확장 초기화
        )
        loadCards()
    }

    private fun loadCards() {
        // 이전 collect Job 취소
        cardsJob?.cancel()

        // 새로운 collect Job 시작
        cardsJob = viewModelScope.launch {
            when (uiState.selectedTab) {
                CardTab.ACTIVE -> {
                    // 사용중인 카드 로드
                    launch {
                        databaseHelper.getActiveBalanceCards().collect { cards ->
                            uiState = uiState.copy(balanceCards = cards)
                            expandFirstCardIfNeeded()
                        }
                    }
                    launch {
                        databaseHelper.getActiveGiftCards().collect { cards ->
                            uiState = uiState.copy(giftCards = cards)
                            expandFirstCardIfNeeded()
                        }
                    }
                }
                CardTab.INACTIVE -> {
                    // 사용완료된 카드 로드
                    launch {
                        databaseHelper.getAllBalanceCards().collect { allCards ->
                            val inactiveCards = allCards.filter { !it.isActive }
                            uiState = uiState.copy(balanceCards = inactiveCards)
                        }
                    }
                    launch {
                        databaseHelper.getAllGiftCards().collect { allCards ->
                            val inactiveCards = allCards.filter { !it.isActive }
                            uiState = uiState.copy(giftCards = inactiveCards)
                        }
                    }
                }
            }
        }
    }

    private fun expandFirstCardIfNeeded() {
        // 초기 확장이 아직 안 됐고, 사용중 탭이고, 카드가 있으면 LazyColumn 최상단 카드를 확장
        if (!uiState.isInitialExpansionDone &&
            uiState.selectedTab == CardTab.ACTIVE &&
            (uiState.balanceCards.isNotEmpty() || uiState.giftCards.isNotEmpty())) {

            // LazyColumn 순서: 잔액권 먼저, 그 다음 상품권
            // 따라서 잔액권이 있으면 무조건 잔액권의 첫 번째가 최상단
            val topCardItem = if (uiState.balanceCards.isNotEmpty()) {
                CardItem.Balance(uiState.balanceCards.first())
            } else if (uiState.giftCards.isNotEmpty()) {
                CardItem.Gift(uiState.giftCards.first())
            } else {
                return
            }

            val topCardId = topCardItem.getId()
            uiState = uiState.copy(
                expandedCardId = topCardId,
                isInitialExpansionDone = true
            )

            // 거래 내역 로드
            loadTransactionsForCard(topCardItem)
        }
    }

    fun toggleCardExpansion(cardItem: CardItem) {
        val cardId = cardItem.getId()

        if (uiState.expandedCardId == cardId) {
            // 이미 확장된 카드를 다시 클릭하면 축소
            uiState = uiState.copy(expandedCardId = null)
        } else {
            // 새로운 카드를 확장
            uiState = uiState.copy(expandedCardId = cardId)

            // 거래 내역이 아직 로드되지 않았으면 로드
            if (!uiState.cardTransactions.containsKey(cardId)) {
                loadTransactionsForCard(cardItem)
            }
        }
    }

    private fun loadTransactionsForCard(cardItem: CardItem) {
        viewModelScope.launch {
            when (cardItem) {
                is CardItem.Balance -> {
                    databaseHelper.getTransactionsByBalanceCard(cardItem.card.id).collect { transactions ->
                        uiState = uiState.copy(
                            cardTransactions = uiState.cardTransactions + (cardItem.card.id to transactions)
                        )
                    }
                }
                is CardItem.Gift -> {
                    databaseHelper.getTransactionsByGiftCard(cardItem.card.id).collect { transactions ->
                        uiState = uiState.copy(
                            cardTransactions = uiState.cardTransactions + (cardItem.card.id to transactions)
                        )
                    }
                }
            }
        }
    }
}
