package com.woojin.paymanagement.presentation.recurringtransaction

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.CustomPaymentMethod
import com.woojin.paymanagement.data.RecurringTransaction

data class RecurringTransactionUiState(
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val todayTransactions: List<RecurringTransaction> = emptyList(),  // 오늘 실행할 항목들
    val categories: List<Category> = emptyList(),
    val customPaymentMethods: List<CustomPaymentMethod> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingTransaction: RecurringTransaction? = null
)
