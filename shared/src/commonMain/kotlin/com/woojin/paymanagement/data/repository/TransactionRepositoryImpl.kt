package com.woojin.paymanagement.data.repository

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class TransactionRepositoryImpl(
    private val databaseHelper: DatabaseHelper
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return databaseHelper.getAllTransactions()
    }

    override fun getTransactionsByDate(date: LocalDate): Flow<List<Transaction>> {
        // DatabaseHelper에 이 메서드가 없으므로 getAllTransactions에서 필터링
        return getAllTransactions().map { transactions ->
            transactions.filter { it.date == date }
        }
    }

    override fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>> {
        // DatabaseHelper에 이 메서드가 없으므로 getAllTransactions에서 필터링
        return getAllTransactions().map { transactions ->
            transactions.filter { it.date >= startDate && it.date <= endDate }
        }
    }

    override suspend fun getTransactionById(transactionId: String): Transaction? {
        // getAllTransactions에서 ID로 찾기
        return databaseHelper.getAllTransactions().map { transactions ->
            transactions.find { it.id == transactionId }
        }.first()
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        databaseHelper.insertTransaction(transaction)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        databaseHelper.updateTransaction(transaction)
    }

    override suspend fun deleteTransaction(transactionId: String) {
        databaseHelper.deleteTransaction(transactionId)
    }

    override suspend fun updateTransactionsCategoryName(oldCategoryName: String, newCategoryName: String) {
        databaseHelper.updateTransactionsCategoryName(oldCategoryName, newCategoryName)
    }

    override suspend fun getOldestTransactionDate(): LocalDate? {
        return databaseHelper.getOldestTransactionDate()
    }
}