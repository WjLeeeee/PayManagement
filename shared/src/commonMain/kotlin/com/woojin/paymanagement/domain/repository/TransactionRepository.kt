package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.data.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDate(date: LocalDate): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>>
    suspend fun getTransactionById(transactionId: String): Transaction?
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transactionId: String)
    suspend fun updateTransactionsCategoryName(oldCategoryName: String, newCategoryName: String)
    suspend fun getOldestTransactionDate(): LocalDate?
}