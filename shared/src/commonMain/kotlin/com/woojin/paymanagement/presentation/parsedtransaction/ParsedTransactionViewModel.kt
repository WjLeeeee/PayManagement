package com.woojin.paymanagement.presentation.parsedtransaction

import com.woojin.paymanagement.domain.usecase.CleanupProcessedParsedTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.DeleteParsedTransactionUseCase
import com.woojin.paymanagement.domain.usecase.GetProcessedParsedTransactionsUseCase
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
    private val deleteParsedTransactionUseCase: DeleteParsedTransactionUseCase,
    private val getProcessedParsedTransactionsUseCase: GetProcessedParsedTransactionsUseCase,
    private val cleanupProcessedParsedTransactionsUseCase: CleanupProcessedParsedTransactionsUseCase
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _uiState = MutableStateFlow(ParsedTransactionUiState())
    val uiState: StateFlow<ParsedTransactionUiState> = _uiState.asStateFlow()

    init {
        loadParsedTransactions()
        loadProcessedTransactions()
    }

    /**
     * 기록 완료 탭: 보관 기간(3일)이 지난 항목을 먼저 지우고, 남은 완료 항목을 구독
     */
    private fun loadProcessedTransactions() {
        viewModelScope.launch {
            try {
                cleanupProcessedParsedTransactionsUseCase()
            } catch (e: Exception) {
                // 정리 실패는 화면 표시에 영향 없음
            }
            getProcessedParsedTransactionsUseCase()
                .catch { /* 완료 목록 오류는 무시 (대기 목록 표시 우선) */ }
                .collect { transactions ->
                    _uiState.value = _uiState.value.copy(processedTransactions = transactions)
                }
        }
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