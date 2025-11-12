package com.woojin.paymanagement.presentation.parsedtransaction

import com.woojin.paymanagement.domain.usecase.DeleteParsedTransactionUseCase
import com.woojin.paymanagement.domain.usecase.GetUnprocessedParsedTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.MarkParsedTransactionProcessedUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class ParsedTransactionViewModel(
    private val getUnprocessedParsedTransactionsUseCase: GetUnprocessedParsedTransactionsUseCase,
    private val markParsedTransactionProcessedUseCase: MarkParsedTransactionProcessedUseCase,
    private val deleteParsedTransactionUseCase: DeleteParsedTransactionUseCase
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _uiState = MutableStateFlow(ParsedTransactionUiState())
    val uiState: StateFlow<ParsedTransactionUiState> = _uiState.asStateFlow()

    init {
        loadParsedTransactions()
    }

    private fun loadParsedTransactions() {
        viewModelScope.launch {
            getUnprocessedParsedTransactionsUseCase()
                .onStart {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    )
                }
                .collect { transactions ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        parsedTransactions = transactions,
                        error = null
                    )
                }
        }
    }

    suspend fun markAsProcessed(id: String) {
        try {
            markParsedTransactionProcessedUseCase(id)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to mark as processed: ${e.message}"
            )
        }
    }

    suspend fun deleteParsedTransaction(id: String) {
        try {
            deleteParsedTransactionUseCase(id)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to delete: ${e.message}"
            )
        }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}