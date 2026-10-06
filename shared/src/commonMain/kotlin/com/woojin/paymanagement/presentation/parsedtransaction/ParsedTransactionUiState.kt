package com.woojin.paymanagement.presentation.parsedtransaction

import com.woojin.paymanagement.data.ParsedTransaction

data class ParsedTransactionUiState(
    val isLoading: Boolean = false,
    val parsedTransactions: List<ParsedTransaction> = emptyList(),
    val processedTransactions: List<ParsedTransaction> = emptyList(), // 기록 완료 (3일 보관)
    val error: String? = null
)