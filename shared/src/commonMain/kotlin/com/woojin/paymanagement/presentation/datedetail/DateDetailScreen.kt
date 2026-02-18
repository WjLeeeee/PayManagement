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
import com.woojin.paymanagement.presentation.recurringtransaction.RecurringTransactionDialog
import com.woojin.paymanagement.strings.LocalStrings
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
    onAddTransaction: () -> Unit,
    nativeAdContent: @Composable () -> Unit = {},
    hasNativeAd: Boolean = false
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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 거래도 없고 광고도 없으면 빈 메시지 표시
            if (dayTransactions.isEmpty() && !hasNativeAd) {
                item {
                    EmptyTransactionMessage()
                }
            }

            // 광고가 있을 때만 광고 삽입 위치 결정
            if (hasNativeAd) {
                val adInsertPosition = minOf(3, dayTransactions.size)

                // 광고 이전 거래 아이템 (최대 3개)
                val transactionsBeforeAd = dayTransactions.take(adInsertPosition)
                items(transactionsBeforeAd) { transaction ->
                    TransactionDetailItem(
                        transaction = transaction,
                        isExpanded = uiState.expandedTransactionId == transaction.id,
                        onClick = { viewModel.toggleTransactionExpansion(transaction.id) },
                        onEdit = { onEditTransaction(transaction) },
                        onDelete = {
                            // 삭제 확인 다이얼로그 표시
                            viewModel.showDeleteConfirmation(transaction)
                        },
                        onSaveAsRecurring = { viewModel.showRecurringTransactionDialog(transaction) },
                        availableCategories = uiState.availableCategories
                    )
                }

                // 네이티브 광고
                item {
                    nativeAdContent()
                }

                // 광고 이후 거래 아이템 (3개 이후)
                val transactionsAfterAd = dayTransactions.drop(adInsertPosition)
                items(transactionsAfterAd) { transaction ->
                    TransactionDetailItem(
                        transaction = transaction,
                        isExpanded = uiState.expandedTransactionId == transaction.id,
                        onClick = { viewModel.toggleTransactionExpansion(transaction.id) },
                        onEdit = { onEditTransaction(transaction) },
                        onDelete = {
                            // 삭제 확인 다이얼로그 표시
                            viewModel.showDeleteConfirmation(transaction)
                        },
                        onSaveAsRecurring = { viewModel.showRecurringTransactionDialog(transaction) },
                        availableCategories = uiState.availableCategories
                    )
                }
            } else {
                // 광고 없으면 모든 거래 아이템만 표시
                items(dayTransactions) { transaction ->
                    TransactionDetailItem(
                        transaction = transaction,
                        isExpanded = uiState.expandedTransactionId == transaction.id,
                        onClick = { viewModel.toggleTransactionExpansion(transaction.id) },
                        onEdit = { onEditTransaction(transaction) },
                        onDelete = {
                            // 삭제 확인 다이얼로그 표시
                            viewModel.showDeleteConfirmation(transaction)
                        },
                        onSaveAsRecurring = { viewModel.showRecurringTransactionDialog(transaction) },
                        availableCategories = uiState.availableCategories
                    )
                }
            }
        }
    }

    val strings = LocalStrings.current

    // 삭제 확인 다이얼로그
    if (uiState.transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmation() },
            title = { Text(strings.deleteTransactionTitle) },
            text = { Text(strings.deleteTransactionConfirm) },
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
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // 삭제 불가 다이얼로그
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(strings.cannotDeleteTransaction) },
            text = { Text(uiState.error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text(strings.confirm)
                }
            }
        )
    }

    // 반복거래 저장 다이얼로그
    if (uiState.showRecurringDialog && uiState.recurringTransactionBase != null) {
        RecurringTransactionDialog(
            transaction = uiState.recurringTransactionBase,
            categories = uiState.availableCategories,
            customPaymentMethods = uiState.customPaymentMethods,
            onDismiss = { viewModel.hideRecurringTransactionDialog() },
            onSave = { recurringTransaction ->
                viewModel.saveRecurringTransaction(recurringTransaction)
            }
        )
    }
}