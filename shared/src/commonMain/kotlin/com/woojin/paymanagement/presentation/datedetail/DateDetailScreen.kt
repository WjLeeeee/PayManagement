package com.woojin.paymanagement.presentation.datedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.Transaction
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
fun DateDetailScreen(
    selectedDate: LocalDate?,
    transactions: List<Transaction>,
    viewModel: DateDetailViewModel,
    onBack: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onAddTransaction: () -> Unit
) {
    val uiState = viewModel.uiState
    val scope = rememberCoroutineScope()

    // ViewModel 초기화
    LaunchedEffect(selectedDate) {
        viewModel.initializeDate(selectedDate)
    }

    // 거래 내역 필터링 (기존과 동일한 로직)
    val dayTransactions = selectedDate?.let { date ->
        transactions.filter { it.date == date }
    } ?: emptyList()

    // 일일 요약 계산 (ViewModel 사용)
    LaunchedEffect(dayTransactions) {
        if (selectedDate != null) {
            viewModel.getTransactionsFlow(selectedDate).collect { /* Flow 수집 */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button
        DateDetailHeader(
            selectedDate = selectedDate,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Summary Card
        DailySummaryCard(
            summary = uiState.dailySummary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Transactions list header
        TransactionListHeader(
            transactionCount = dayTransactions.size,
            onAddTransaction = onAddTransaction
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (dayTransactions.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(dayTransactions) { transaction ->
                    TransactionDetailItem(
                        transaction = transaction,
                        onEdit = { onEditTransaction(transaction) },
                        onDelete = {
                            scope.launch {
                                viewModel.deleteTransaction(transaction)
                                onDeleteTransaction(transaction)
                            }
                        }
                    )
                }
            }
        } else {
            EmptyTransactionMessage()
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}