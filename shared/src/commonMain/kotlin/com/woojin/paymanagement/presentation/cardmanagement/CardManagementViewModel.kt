package com.woojin.paymanagement.presentation.cardmanagement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.CustomPaymentMethod
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.usecase.GetCustomPaymentMethodsUseCase
import com.woojin.paymanagement.domain.usecase.AddCustomPaymentMethodUseCase
import com.woojin.paymanagement.domain.usecase.UpdateCustomPaymentMethodUseCase
import com.woojin.paymanagement.domain.usecase.DeleteCustomPaymentMethodUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.benasher44.uuid.uuid4
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CardManagementViewModel(
    private val databaseHelper: DatabaseHelper,
    private val getCustomPaymentMethodsUseCase: GetCustomPaymentMethodsUseCase,
    private val addCustomPaymentMethodUseCase: AddCustomPaymentMethodUseCase,
    private val updateCustomPaymentMethodUseCase: UpdateCustomPaymentMethodUseCase,
    private val deleteCustomPaymentMethodUseCase: DeleteCustomPaymentMethodUseCase
) : ViewModel() {

    var uiState by mutableStateOf(CardManagementUiState())
        private set

    private var cardsJob: Job? = null
    private var methodsJob: Job? = null

    init {
        loadCards()
        loadMethods()
    }

    fun selectTab(tab: CardTab) {
        uiState = uiState.copy(
            selectedTab = tab,
            expandedCardId = null
        )
        if (tab == CardTab.BALANCE) {
            loadCards()
        }
    }

    private fun loadCards() {
        cardsJob?.cancel()
        cardsJob = viewModelScope.launch {
            databaseHelper.getActiveBalanceCards().collect { cards ->
                uiState = uiState.copy(balanceCards = cards)
                expandFirstCardIfNeeded()
            }
        }
    }

    private fun loadMethods() {
        methodsJob?.cancel()
        methodsJob = viewModelScope.launch {
            getCustomPaymentMethodsUseCase().collect { methods ->
                uiState = uiState.copy(customPaymentMethods = methods)
            }
        }
    }

    private fun expandFirstCardIfNeeded() {
        if (!uiState.isInitialExpansionDone &&
            uiState.selectedTab == CardTab.BALANCE &&
            uiState.balanceCards.isNotEmpty()) {

            val topCardItem = CardItem.Balance(uiState.balanceCards.first())

            val topCardId = topCardItem.getId()
            uiState = uiState.copy(
                expandedCardId = topCardId,
                isInitialExpansionDone = true
            )

            loadTransactionsForCard(topCardItem)
        }
    }

    fun toggleCardExpansion(cardItem: CardItem) {
        val cardId = cardItem.getId()

        if (uiState.expandedCardId == cardId) {
            uiState = uiState.copy(expandedCardId = null)
        } else {
            uiState = uiState.copy(expandedCardId = cardId)

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
            }
        }
    }

    // 커스텀 결제수단 CRUD

    fun showAddDialog() {
        uiState = uiState.copy(
            isAddDialogVisible = true,
            newMethodName = ""
        )
    }

    fun hideAddDialog() {
        uiState = uiState.copy(isAddDialogVisible = false)
    }

    fun updateNewMethodName(name: String) {
        uiState = uiState.copy(newMethodName = name)
    }

    fun addMethod() {
        if (uiState.newMethodName.isBlank()) return

        viewModelScope.launch {
            try {
                val isFirstMethod = uiState.customPaymentMethods.isEmpty()
                val newMethod = CustomPaymentMethod(
                    id = uuid4().toString(),
                    name = uiState.newMethodName.trim(),
                    sortOrder = uiState.customPaymentMethods.size,
                    isDefault = isFirstMethod
                )
                addCustomPaymentMethodUseCase(newMethod)
                hideAddDialog()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }

    fun setDefaultMethod(method: CustomPaymentMethod) {
        viewModelScope.launch {
            try {
                databaseHelper.clearAllDefaultPaymentMethods()
                updateCustomPaymentMethodUseCase(
                    method,
                    method.copy(isDefault = true)
                )
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }

    fun showEditDialog(method: CustomPaymentMethod) {
        uiState = uiState.copy(
            isEditDialogVisible = true,
            editingMethod = method,
            editMethodName = method.name
        )
    }

    fun hideEditDialog() {
        uiState = uiState.copy(
            isEditDialogVisible = false,
            editingMethod = null,
            editMethodName = ""
        )
    }

    fun updateEditMethodName(name: String) {
        uiState = uiState.copy(editMethodName = name)
    }

    fun updateMethod() {
        val editingMethod = uiState.editingMethod ?: return
        if (uiState.editMethodName.isBlank()) return

        val newMethod = editingMethod.copy(name = uiState.editMethodName.trim())
        val nameChanged = editingMethod.name != newMethod.name

        if (nameChanged) {
            uiState = uiState.copy(
                showConfirmDialog = true,
                confirmDialogMessage = "결제수단 이름을 변경하면 해당 카드로 저장된 모든 거래 내역의 카드 이름도 함께 변경됩니다.\n\n변경 전: ${editingMethod.name}\n변경 후: ${newMethod.name}\n\n계속하시겠습니까?",
                pendingUpdate = { performUpdate(editingMethod, newMethod) }
            )
        } else {
            performUpdate(editingMethod, newMethod)
        }
    }

    private fun performUpdate(oldMethod: CustomPaymentMethod, newMethod: CustomPaymentMethod) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true)
                updateCustomPaymentMethodUseCase(oldMethod, newMethod)
                hideEditDialog()
                hideConfirmDialog()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun showConfirmDialogForUpdate() {
        uiState.pendingUpdate?.invoke()
    }

    fun hideConfirmDialog() {
        uiState = uiState.copy(
            showConfirmDialog = false,
            confirmDialogMessage = "",
            pendingUpdate = null
        )
    }

    fun showDeleteConfirmDialog(method: CustomPaymentMethod) {
        uiState = uiState.copy(
            isDeleteDialogVisible = true,
            deletingMethod = method
        )
    }

    fun hideDeleteConfirmDialog() {
        uiState = uiState.copy(
            isDeleteDialogVisible = false,
            deletingMethod = null
        )
    }

    fun confirmDelete() {
        val methodToDelete = uiState.deletingMethod ?: return

        viewModelScope.launch {
            try {
                deleteCustomPaymentMethodUseCase(methodToDelete.id)

                // 삭제한 카드가 기본 카드였으면 남은 첫 번째 카드를 기본으로 설정
                if (methodToDelete.isDefault) {
                    val remaining = uiState.customPaymentMethods.filter { it.id != methodToDelete.id }
                    remaining.firstOrNull()?.let { firstRemaining ->
                        databaseHelper.clearAllDefaultPaymentMethods()
                        val updated = firstRemaining.copy(isDefault = true)
                        databaseHelper.updateCustomPaymentMethod(updated)
                    }
                }

                hideDeleteConfirmDialog()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
                hideDeleteConfirmDialog()
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    // 잔액권 추가

    fun showAddBalanceCardDialog() {
        uiState = uiState.copy(
            isAddBalanceCardDialogVisible = true,
            newBalanceCardName = "",
            newBalanceCardAmount = ""
        )
    }

    fun hideAddBalanceCardDialog() {
        uiState = uiState.copy(isAddBalanceCardDialogVisible = false)
    }

    fun updateNewBalanceCardName(name: String) {
        uiState = uiState.copy(newBalanceCardName = name)
    }

    fun updateNewBalanceCardAmount(amount: String) {
        uiState = uiState.copy(newBalanceCardAmount = amount)
    }

    fun addBalanceCard() {
        if (uiState.newBalanceCardName.isBlank()) return
        val amount = uiState.newBalanceCardAmount.toLongOrNull() ?: return
        viewModelScope.launch {
            try {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val newCard = BalanceCard(
                    id = uuid4().toString(),
                    name = uiState.newBalanceCardName.trim(),
                    initialAmount = amount.toDouble(),
                    currentBalance = amount.toDouble(),
                    createdDate = today
                )
                databaseHelper.insertBalanceCard(newCard)
                hideAddBalanceCardDialog()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }

    // 잔액권 수정/삭제

    fun showEditBalanceCardDialog(card: BalanceCard) {
        uiState = uiState.copy(
            isEditBalanceCardDialogVisible = true,
            editingBalanceCard = card,
            editBalanceCardName = card.name,
            editBalanceCardCurrentBalance = card.currentBalance.toLong().toString()
        )
    }

    fun hideEditBalanceCardDialog() {
        uiState = uiState.copy(
            isEditBalanceCardDialogVisible = false,
            editingBalanceCard = null,
            editBalanceCardName = "",
            editBalanceCardCurrentBalance = ""
        )
    }

    fun updateEditBalanceCardName(name: String) {
        uiState = uiState.copy(editBalanceCardName = name)
    }

    fun updateEditBalanceCardCurrentBalance(amount: String) {
        uiState = uiState.copy(editBalanceCardCurrentBalance = amount)
    }

    fun updateBalanceCard() {
        val card = uiState.editingBalanceCard ?: return
        if (uiState.editBalanceCardName.isBlank()) return
        val newBalance = uiState.editBalanceCardCurrentBalance.toLongOrNull()?.toDouble()
            ?: card.currentBalance
        viewModelScope.launch {
            try {
                val newName = uiState.editBalanceCardName.trim()
                databaseHelper.updateBalanceCard(
                    card.copy(name = newName, currentBalance = newBalance)
                )
                // 해당 잔액권의 모든 거래내역 cardName도 업데이트
                databaseHelper.updateTransactionCardNameByBalanceCardId(card.id, newName)
                hideEditBalanceCardDialog()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }

    fun showDeleteBalanceCardDialog(card: BalanceCard) {
        uiState = uiState.copy(
            isDeleteBalanceCardDialogVisible = true,
            deletingBalanceCard = card
        )
    }

    fun hideDeleteBalanceCardDialog() {
        uiState = uiState.copy(
            isDeleteBalanceCardDialogVisible = false,
            deletingBalanceCard = null
        )
    }

    fun confirmDeleteBalanceCard() {
        val card = uiState.deletingBalanceCard ?: return
        viewModelScope.launch {
            try {
                databaseHelper.deleteBalanceCard(card.id)
                hideDeleteBalanceCardDialog()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
                hideDeleteBalanceCardDialog()
            }
        }
    }
}
