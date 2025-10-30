package com.woojin.paymanagement.presentation.datedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.utils.BackHandler
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
    // 시스템 뒤로가기 버튼 처리
    BackHandler(onBack = onBack)

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
            .windowInsetsPadding(WindowInsets.systemBars)
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(dayTransactions) { transaction ->
                    TransactionDetailItem(
                        transaction = transaction,
                        onEdit = { onEditTransaction(transaction) },
                        onDelete = {
                            // 삭제 확인 다이얼로그 표시
                            viewModel.showDeleteConfirmation(transaction)
                        },
                        availableCategories = uiState.availableCategories
                    )
                }
            }
        } else {
            EmptyTransactionMessage()
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    // 삭제 확인 다이얼로그
    if (uiState.transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmation() },
            title = { Text("거래 삭제") },
            text = { Text("이 거래를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val transaction = uiState.transactionToDelete!!
                            viewModel.dismissDeleteConfirmation()
                            val success = viewModel.deleteTransaction(transaction)
                            // 삭제 성공 시에만 콜백 호출
                            if (success) {
                                onDeleteTransaction(transaction)
                            }
                        }
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                    Text("취소")
                }
            }
        )
    }

    // 삭제 불가 다이얼로그
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("거래 삭제 불가") },
            text = { Text(uiState.error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("확인")
                }
            }
        )
    }
}