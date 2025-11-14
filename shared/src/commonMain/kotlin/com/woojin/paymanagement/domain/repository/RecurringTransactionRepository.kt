package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.data.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>>
    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction)
    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction)
    suspend fun updateLastExecutedDate(id: String, date: String)
    suspend fun deleteRecurringTransaction(id: String)
}
