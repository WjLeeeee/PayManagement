package com.woojin.paymanagement.data.repository

import com.woojin.paymanagement.data.RecurringTransaction
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow

class RecurringTransactionRepositoryImpl(
    private val databaseHelper: DatabaseHelper
) : RecurringTransactionRepository {

    override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return databaseHelper.getAllRecurringTransactions()
    }

    override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return databaseHelper.getActiveRecurringTransactions()
    }

    override suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction) {
        databaseHelper.insertRecurringTransaction(recurringTransaction)
    }

    override suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction) {
        databaseHelper.updateRecurringTransaction(recurringTransaction)
    }

    override suspend fun updateLastExecutedDate(id: String, date: String) {
        databaseHelper.updateRecurringTransactionLastExecuted(id, date)
    }

    override suspend fun deleteRecurringTransaction(id: String) {
        databaseHelper.deleteRecurringTransaction(id)
    }
}
