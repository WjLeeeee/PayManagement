package com.woojin.paymanagement.presentation.parsedtransaction

import com.woojin.paymanagement.data.ParsedTransaction

data class ParsedTransactionUiState(
    val isLoading: Boolean = false,
    val parsedTransactions: List<ParsedTransaction> = emptyList(),
    val error: String? = null
)