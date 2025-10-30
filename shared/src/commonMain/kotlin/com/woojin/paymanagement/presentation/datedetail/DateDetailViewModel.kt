package com.woojin.paymanagement.presentation.datedetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.usecase.CalculateDailySummaryUseCase
import com.woojin.paymanagement.domain.usecase.DeleteTransactionUseCase
import com.woojin.paymanagement.domain.usecase.GetTransactionsByDateUseCase
import com.woojin.paymanagement.domain.usecase.GetCategoriesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class DateDetailViewModel(
    private val getTransactionsByDateUseCase: GetTransactionsByDateUseCase,
    private val calculateDailySummaryUseCase: CalculateDailySummaryUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val coroutineScope: CoroutineScope
) {
    var uiState by mutableStateOf(DateDetailUiState())
        private set

    init {
        // 카테고리 목록을 로드하여 UiState에 반영
        coroutineScope.launch {
            combine(
                getCategoriesUseCase(TransactionType.INCOME),
                getCategoriesUseCase(TransactionType.EXPENSE)
            ) { income, expense ->
                income + expense
            }.collect { categories ->
                uiState = uiState.copy(availableCategories = categories)
            }
        }
    }

    fun initializeDate(date: LocalDate?) {
        uiState = uiState.copy(selectedDate = date)
    }

    fun getTransactionsFlow(date: LocalDate): Flow<List<Transaction>> {
        return getTransactionsByDateUseCase(date).map { transactions ->
            val summary = calculateDailySummaryUseCase(transactions)
            uiState = uiState.copy(
                transactions = transactions,
                dailySummary = summary
            )
            transactions
        }
    }

    suspend fun deleteTransaction(transaction: Transaction): Boolean {
        try {
            uiState = uiState.copy(isLoading = true, error = null)

            // 1. 삭제 가능 여부 먼저 검증
            val validationResult = deleteTransactionUseCase.validateDeletion(transaction.id)

            if (!validationResult.canDelete) {
                // 삭제 불가능 - 에러 메시지만 표시하고 삭제 안 함
                uiState = uiState.copy(
                    isLoading = false,
                    error = validationResult.errorMessage ?: "거래를 삭제할 수 없습니다."
                )
                return false
            }

            // 2. 검증 통과 - 실제 삭제 실행
            deleteTransactionUseCase(transaction.id)
            uiState = uiState.copy(isLoading = false)
            return true // 삭제 성공
        } catch (e: Exception) {
            uiState = uiState.copy(
                isLoading = false,
                error = e.message ?: "거래 삭제 중 오류가 발생했습니다."
            )
            return false // 삭제 실패
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    fun showDeleteConfirmation(transaction: Transaction) {
        uiState = uiState.copy(transactionToDelete = transaction)
    }

    fun dismissDeleteConfirmation() {
        uiState = uiState.copy(transactionToDelete = null)
    }

    fun toggleTransactionExpansion(transactionId: String) {
        uiState = uiState.copy(
            expandedTransactionId = if (uiState.expandedTransactionId == transactionId) null else transactionId
        )
    }
}