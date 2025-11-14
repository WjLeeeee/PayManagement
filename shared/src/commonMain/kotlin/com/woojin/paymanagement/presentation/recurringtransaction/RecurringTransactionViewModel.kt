package com.woojin.paymanagement.presentation.recurringtransaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.data.RecurringTransaction
import com.woojin.paymanagement.domain.repository.CategoryRepository
import com.woojin.paymanagement.domain.usecase.CheckTodayRecurringTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.DeleteRecurringTransactionUseCase
import com.woojin.paymanagement.domain.usecase.GetRecurringTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.SaveRecurringTransactionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class RecurringTransactionViewModel(
    private val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    private val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val checkTodayRecurringTransactionsUseCase: CheckTodayRecurringTransactionsUseCase,
    private val categoryRepository: CategoryRepository,
    private val coroutineScope: CoroutineScope
) {
    var uiState by mutableStateOf(RecurringTransactionUiState())
        private set

    init {
        loadData()
    }

    private fun loadData() {
        coroutineScope.launch {
            uiState = uiState.copy(isLoading = true)

            combine(
                getRecurringTransactionsUseCase(activeOnly = false),
                checkTodayRecurringTransactionsUseCase(),
                categoryRepository.getAllCategories()
            ) { recurring, today, categories ->
                Triple(recurring, today, categories)
            }.collect { (recurring, today, categories) ->
                uiState = uiState.copy(
                    recurringTransactions = recurring,
                    todayTransactions = today,
                    categories = categories,
                    isLoading = false
                )
            }
        }
    }

    fun showAddDialog() {
        uiState = uiState.copy(
            showAddDialog = true,
            editingTransaction = null
        )
    }

    fun showEditDialog(transaction: RecurringTransaction) {
        uiState = uiState.copy(
            showAddDialog = true,
            editingTransaction = transaction
        )
    }

    fun hideDialog() {
        uiState = uiState.copy(
            showAddDialog = false,
            editingTransaction = null
        )
    }

    fun saveRecurringTransaction(transaction: RecurringTransaction) {
        coroutineScope.launch {
            val isUpdate = uiState.editingTransaction != null
            saveRecurringTransactionUseCase(transaction, isUpdate)
            hideDialog()
        }
    }

    fun deleteRecurringTransaction(id: String) {
        coroutineScope.launch {
            deleteRecurringTransactionUseCase(id)
        }
    }

    fun toggleActive(transaction: RecurringTransaction) {
        coroutineScope.launch {
            val updated = transaction.copy(isActive = !transaction.isActive)
            saveRecurringTransactionUseCase(updated, isUpdate = true)
        }
    }
}
