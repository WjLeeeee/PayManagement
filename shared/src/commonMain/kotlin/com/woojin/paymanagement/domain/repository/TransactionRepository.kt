package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.GiftCard
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDate(date: LocalDate): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transactionId: String)

    // BalanceCard methods
    fun getActiveBalanceCards(): Flow<List<BalanceCard>>
    suspend fun insertBalanceCard(balanceCard: BalanceCard)
    suspend fun getBalanceCardById(id: String): BalanceCard?
    suspend fun updateBalanceCardBalance(id: String, currentBalance: Double, isActive: Boolean)

    // GiftCard methods
    fun getActiveGiftCards(): Flow<List<GiftCard>>
    suspend fun insertGiftCard(giftCard: GiftCard)
    suspend fun getGiftCardById(id: String): GiftCard?
    suspend fun updateGiftCardUsage(id: String, usedAmount: Double, isActive: Boolean)
}