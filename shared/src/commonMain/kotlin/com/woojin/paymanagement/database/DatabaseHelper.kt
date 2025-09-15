package com.woojin.paymanagement.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.GiftCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class DatabaseHelper(
    private val database: PayManagementDatabase
) {
    private val queries = database.transactionQueries
    
    fun getAllTransactions(): Flow<List<Transaction>> {
        return queries.selectAllTransactions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toTransaction() }
            }
    }
    
    fun getTransactionsByDate(date: LocalDate): Flow<List<Transaction>> {
        return queries.selectTransactionsByDate(date.toString())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toTransaction() }
            }
    }
    
    fun getTransactionsByMonth(yearMonth: String): Flow<List<Transaction>> {
        return queries.selectTransactionsByMonth(yearMonth)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toTransaction() }
            }
    }
    
    suspend fun insertTransaction(transaction: Transaction) {
        queries.insertTransaction(
            id = transaction.id,
            amount = transaction.amount,
            type = transaction.type.name,
            category = transaction.category,
            memo = transaction.memo,
            date = transaction.date.toString(),
            incomeType = transaction.incomeType?.name,
            paymentMethod = transaction.paymentMethod?.name,
            balanceCardId = transaction.balanceCardId,
            giftCardId = transaction.giftCardId,
            cardName = transaction.cardName
        )
    }
    
    suspend fun updateTransaction(transaction: Transaction) {
        queries.updateTransaction(
            amount = transaction.amount,
            type = transaction.type.name,
            category = transaction.category,
            memo = transaction.memo,
            date = transaction.date.toString(),
            incomeType = transaction.incomeType?.name,
            paymentMethod = transaction.paymentMethod?.name,
            balanceCardId = transaction.balanceCardId,
            giftCardId = transaction.giftCardId,
            cardName = transaction.cardName,
            id = transaction.id
        )
    }
    
    suspend fun deleteTransaction(id: String) {
        queries.deleteTransaction(id)
    }
    
    suspend fun deleteAllTransactions() {
        queries.deleteAllTransactions()
    }
    
    // BalanceCard 관련 메서드들
    fun getAllBalanceCards(): Flow<List<BalanceCard>> {
        return queries.selectAllBalanceCards()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toBalanceCard() }
            }
    }

    fun getActiveBalanceCards(): Flow<List<BalanceCard>> {
        return queries.selectActiveBalanceCards()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toBalanceCard() }
            }
    }

    suspend fun insertBalanceCard(balanceCard: BalanceCard) {
        queries.insertBalanceCard(
            id = balanceCard.id,
            name = balanceCard.name,
            initialAmount = balanceCard.initialAmount,
            currentBalance = balanceCard.currentBalance,
            createdDate = balanceCard.createdDate.toString(),
            isActive = if (balanceCard.isActive) 1L else 0L
        )
    }

    suspend fun updateBalanceCard(balanceCard: BalanceCard) {
        queries.updateBalanceCard(
            name = balanceCard.name,
            initialAmount = balanceCard.initialAmount,
            currentBalance = balanceCard.currentBalance,
            createdDate = balanceCard.createdDate.toString(),
            isActive = if (balanceCard.isActive) 1L else 0L,
            id = balanceCard.id
        )
    }

    suspend fun getBalanceCardById(id: String): BalanceCard? {
        return queries.selectBalanceCardById(id).executeAsOneOrNull()?.toBalanceCard()
    }

    suspend fun updateBalanceCardBalance(id: String, currentBalance: Double, isActive: Boolean) {
        queries.updateBalanceCardBalance(
            currentBalance = currentBalance,
            isActive = if (isActive) 1L else 0L,
            id = id
        )
    }

    suspend fun deleteBalanceCard(id: String) {
        queries.deleteBalanceCard(id)
    }

    // GiftCard 관련 메서드들
    fun getAllGiftCards(): Flow<List<GiftCard>> {
        return queries.selectAllGiftCards()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toGiftCard() }
            }
    }

    fun getActiveGiftCards(): Flow<List<GiftCard>> {
        return queries.selectActiveGiftCards()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toGiftCard() }
            }
    }

    suspend fun insertGiftCard(giftCard: GiftCard) {
        queries.insertGiftCard(
            id = giftCard.id,
            name = giftCard.name,
            totalAmount = giftCard.totalAmount,
            usedAmount = giftCard.usedAmount,
            createdDate = giftCard.createdDate.toString(),
            isActive = if (giftCard.isActive) 1L else 0L,
            minimumUsageRate = giftCard.minimumUsageRate
        )
    }

    suspend fun updateGiftCard(giftCard: GiftCard) {
        queries.updateGiftCard(
            name = giftCard.name,
            totalAmount = giftCard.totalAmount,
            usedAmount = giftCard.usedAmount,
            createdDate = giftCard.createdDate.toString(),
            isActive = if (giftCard.isActive) 1L else 0L,
            minimumUsageRate = giftCard.minimumUsageRate,
            id = giftCard.id
        )
    }

    suspend fun getGiftCardById(id: String): GiftCard? {
        return queries.selectGiftCardById(id).executeAsOneOrNull()?.toGiftCard()
    }

    suspend fun updateGiftCardUsage(id: String, usedAmount: Double, isActive: Boolean) {
        queries.updateGiftCardUsage(
            usedAmount = usedAmount,
            isActive = if (isActive) 1L else 0L,
            id = id
        )
    }

    suspend fun deleteGiftCard(id: String) {
        queries.deleteGiftCard(id)
    }

    private fun TransactionEntity.toTransaction(): Transaction {
        return Transaction(
            id = this.id,
            amount = this.amount,
            type = TransactionType.valueOf(this.type),
            category = this.category,
            memo = this.memo,
            date = LocalDate.parse(this.date),
            incomeType = this.incomeType?.let { IncomeType.valueOf(it) },
            paymentMethod = this.paymentMethod?.let { PaymentMethod.valueOf(it) },
            balanceCardId = this.balanceCardId,
            giftCardId = this.giftCardId,
            cardName = this.cardName
        )
    }

    private fun BalanceCardEntity.toBalanceCard(): BalanceCard {
        return BalanceCard(
            id = this.id,
            name = this.name,
            initialAmount = this.initialAmount,
            currentBalance = this.currentBalance,
            createdDate = LocalDate.parse(this.createdDate),
            isActive = this.isActive == 1L
        )
    }

    private fun GiftCardEntity.toGiftCard(): GiftCard {
        return GiftCard(
            id = this.id,
            name = this.name,
            totalAmount = this.totalAmount,
            usedAmount = this.usedAmount,
            createdDate = LocalDate.parse(this.createdDate),
            isActive = this.isActive == 1L,
            minimumUsageRate = this.minimumUsageRate
        )
    }
}