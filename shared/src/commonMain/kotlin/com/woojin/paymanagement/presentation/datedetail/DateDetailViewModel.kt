package com.woojin.paymanagement.presentation.datedetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.usecase.CalculateDailySummaryUseCase
import com.woojin.paymanagement.domain.usecase.DeleteTransactionUseCase
import com.woojin.paymanagement.domain.usecase.GetTransactionsByDateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class DateDetailViewModel(
    private val getTransactionsByDateUseCase: GetTransactionsByDateUseCase,
    private val calculateDailySummaryUseCase: CalculateDailySummaryUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) {
    var uiState by mutableStateOf(DateDetailUiState())
        private set

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

    suspend fun deleteTransaction(transaction: Transaction) {
        try {
            uiState = uiState.copy(isLoading = true, error = null)
            deleteTransactionUseCase(transaction.id)
            uiState = uiState.copy(isLoading = false)
        } catch (e: Exception) {
            uiState = uiState.copy(
                isLoading = false,
                error = e.message ?: "거래 삭제 중 오류가 발생했습니다."
            )
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}