package com.woojin.paymanagement.data.repository

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
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

    override suspend fun insertTransaction(transaction: Transaction) {
        databaseHelper.insertTransaction(transaction)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        databaseHelper.updateTransaction(transaction)
    }

    override suspend fun deleteTransaction(transactionId: String) {
        databaseHelper.deleteTransaction(transactionId)
    }

    // BalanceCard methods
    override fun getActiveBalanceCards(): Flow<List<BalanceCard>> {
        return databaseHelper.getActiveBalanceCards()
    }

    override suspend fun insertBalanceCard(balanceCard: BalanceCard) {
        databaseHelper.insertBalanceCard(balanceCard)
    }

    override suspend fun getBalanceCardById(id: String): BalanceCard? {
        return databaseHelper.getBalanceCardById(id)
    }

    override suspend fun updateBalanceCardBalance(id: String, currentBalance: Double, isActive: Boolean) {
        databaseHelper.updateBalanceCardBalance(id, currentBalance, isActive)
    }

    // GiftCard methods
    override fun getActiveGiftCards(): Flow<List<GiftCard>> {
        return databaseHelper.getActiveGiftCards()
    }

    override suspend fun insertGiftCard(giftCard: GiftCard) {
        databaseHelper.insertGiftCard(giftCard)
    }

    override suspend fun getGiftCardById(id: String): GiftCard? {
        return databaseHelper.getGiftCardById(id)
    }

    override suspend fun updateGiftCardUsage(id: String, usedAmount: Double, isActive: Boolean) {
        databaseHelper.updateGiftCardUsage(id, usedAmount, isActive)
    }
}