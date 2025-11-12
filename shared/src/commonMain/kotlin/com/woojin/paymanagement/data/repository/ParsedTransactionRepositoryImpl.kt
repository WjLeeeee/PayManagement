package com.woojin.paymanagement.data.repository

import com.woojin.paymanagement.data.ParsedTransaction
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.ParsedTransactionRepository
import kotlinx.coroutines.flow.Flow

class ParsedTransactionRepositoryImpl(
    private val databaseHelper: DatabaseHelper
) : ParsedTransactionRepository {

    override fun getAllParsedTransactions(): Flow<List<ParsedTransaction>> {
        return databaseHelper.getAllParsedTransactions()
    }

    override fun getUnprocessedParsedTransactions(): Flow<List<ParsedTransaction>> {
        return databaseHelper.getUnprocessedParsedTransactions()
    }

    override suspend fun insertParsedTransaction(parsedTransaction: ParsedTransaction) {
        databaseHelper.insertParsedTransaction(parsedTransaction)
    }

    override suspend fun markAsProcessed(id: String) {
        databaseHelper.markParsedTransactionAsProcessed(id)
    }

    override suspend fun deleteParsedTransaction(id: String) {
        databaseHelper.deleteParsedTransaction(id)
    }

    override suspend fun deleteAll() {
        databaseHelper.deleteAllParsedTransactions()
    }

    override suspend fun hasRecentTransactionWithAmount(amount: Double, startTime: Long, endTime: Long): Boolean {
        return databaseHelper.hasRecentTransactionWithAmount(amount, startTime, endTime)
    }
}