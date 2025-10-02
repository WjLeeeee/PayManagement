package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.data.ParsedTransaction
import kotlinx.coroutines.flow.Flow

interface ParsedTransactionRepository {
    fun getAllParsedTransactions(): Flow<List<ParsedTransaction>>
    fun getUnprocessedParsedTransactions(): Flow<List<ParsedTransaction>>
    suspend fun insertParsedTransaction(parsedTransaction: ParsedTransaction)
    suspend fun markAsProcessed(id: String)
    suspend fun deleteParsedTransaction(id: String)
    suspend fun deleteAll()
}