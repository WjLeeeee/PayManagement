package com.woojin.paymanagement.presentation.parsedtransaction

import com.woojin.paymanagement.data.ParsedTransaction
import com.woojin.paymanagement.domain.usecase.DeleteParsedTransactionUseCase
import com.woojin.paymanagement.domain.usecase.GetUnprocessedParsedTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.InsertParsedTransactionUseCase
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
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class ParsedTransactionViewModel(
    private val getUnprocessedParsedTransactionsUseCase: GetUnprocessedParsedTransactionsUseCase,
    private val markParsedTransactionProcessedUseCase: MarkParsedTransactionProcessedUseCase,
    private val deleteParsedTransactionUseCase: DeleteParsedTransactionUseCase,
    private val insertParsedTransactionUseCase: InsertParsedTransactionUseCase
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

    suspend fun addTestData() {
        try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val testTransactions = listOf(
                ParsedTransaction(
                    id = "test_${Clock.System.now().toEpochMilliseconds()}_1",
                    amount = 7818.0,
                    merchantName = "(주)비바리퍼블리카",
                    date = now.date,
                    rawNotification = "[신한카드] 테스트 데이터",
                    isProcessed = false,
                    createdAt = Clock.System.now().toEpochMilliseconds()
                ),
                ParsedTransaction(
                    id = "test_${Clock.System.now().toEpochMilliseconds()}_2",
                    amount = 15000.0,
                    merchantName = "스타벅스 강남점",
                    date = now.date.plus(1, DateTimeUnit.DAY),
                    rawNotification = "[신한카드] 테스트 데이터",
                    isProcessed = false,
                    createdAt = Clock.System.now().toEpochMilliseconds() + 1
                ),
                ParsedTransaction(
                    id = "test_${Clock.System.now().toEpochMilliseconds()}_3",
                    amount = 35000.0,
                    merchantName = "CU편의점",
                    date = now.date.plus(10, DateTimeUnit.DAY),
                    rawNotification = "[신한카드] 테스트 데이터",
                    isProcessed = false,
                    createdAt = Clock.System.now().toEpochMilliseconds() + 2
                )
            )

            testTransactions.forEach { transaction ->
                insertParsedTransactionUseCase(transaction)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to add test data: ${e.message}"
            )
        }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}