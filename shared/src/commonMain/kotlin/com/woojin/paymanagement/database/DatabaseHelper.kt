package com.woojin.paymanagement.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.ParsedTransaction
import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.BudgetPlan
import com.woojin.paymanagement.data.CategoryBudget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class DatabaseHelper(
    private val database: PayManagementDatabase
) {
    private val queries = database.transactionQueries
    private val json = Json { ignoreUnknownKeys = true }
    
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

    suspend fun getOldestTransactionDate(): LocalDate? {
        return try {
            val dateString = queries.selectOldestTransactionDate().executeAsOneOrNull()?.oldestDate
            dateString?.let { LocalDate.parse(it) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun insertTransaction(transaction: Transaction) {
        queries.insertTransaction(
            id = transaction.id,
            amount = transaction.amount,
            type = transaction.type.name,
            category = transaction.category,
            merchant = transaction.merchant,
            memo = transaction.memo,
            date = transaction.date.toString(),
            incomeType = transaction.incomeType?.name,
            paymentMethod = transaction.paymentMethod?.name,
            balanceCardId = transaction.balanceCardId,
            giftCardId = transaction.giftCardId,
            cardName = transaction.cardName,
            actualAmount = transaction.actualAmount,
            settlementAmount = transaction.settlementAmount,
            isSettlement = if (transaction.isSettlement) 1L else 0L
        )
    }
    
    suspend fun updateTransaction(transaction: Transaction) {
        queries.updateTransaction(
            amount = transaction.amount,
            type = transaction.type.name,
            category = transaction.category,
            merchant = transaction.merchant,
            memo = transaction.memo,
            date = transaction.date.toString(),
            incomeType = transaction.incomeType?.name,
            paymentMethod = transaction.paymentMethod?.name,
            balanceCardId = transaction.balanceCardId,
            giftCardId = transaction.giftCardId,
            cardName = transaction.cardName,
            actualAmount = transaction.actualAmount,
            settlementAmount = transaction.settlementAmount,
            isSettlement = if (transaction.isSettlement) 1L else 0L,
            id = transaction.id
        )
    }
    
    suspend fun deleteTransaction(id: String) {
        queries.deleteTransaction(id)
    }

    suspend fun deleteAllTransactions() {
        queries.deleteAllTransactions()
    }

    suspend fun updateTransactionsCategoryName(oldCategoryName: String, newCategoryName: String) {
        queries.updateTransactionsCategoryName(newCategoryName, oldCategoryName)
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

    suspend fun getTotalExpenseByBalanceCard(balanceCardId: String): Double {
        return queries.getTotalExpenseByBalanceCard(balanceCardId).executeAsOne().totalExpense ?: 0.0
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

    suspend fun deleteAllBalanceCards() {
        queries.deleteAllBalanceCards()
    }

    fun getTransactionsByBalanceCard(balanceCardId: String): Flow<List<Transaction>> {
        return queries.selectTransactionsByBalanceCard(balanceCardId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toTransaction() }
            }
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

    suspend fun deleteAllGiftCards() {
        queries.deleteAllGiftCards()
    }

    fun getTransactionsByGiftCard(giftCardId: String): Flow<List<Transaction>> {
        return queries.selectTransactionsByGiftCard(giftCardId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toTransaction() }
            }
    }

    // ParsedTransaction 관련 메서드들
    fun getAllParsedTransactions(): Flow<List<ParsedTransaction>> {
        return queries.selectAllParsedTransactions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toParsedTransaction() }
            }
    }

    fun getUnprocessedParsedTransactions(): Flow<List<ParsedTransaction>> {
        return queries.selectUnprocessedParsedTransactions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toParsedTransaction() }
            }
    }

    suspend fun insertParsedTransaction(parsedTransaction: ParsedTransaction) {
        queries.insertParsedTransaction(
            id = parsedTransaction.id,
            amount = parsedTransaction.amount,
            merchantName = parsedTransaction.merchantName,
            date = parsedTransaction.date.toString(),
            rawNotification = parsedTransaction.rawNotification,
            isProcessed = if (parsedTransaction.isProcessed) 1L else 0L,
            createdAt = parsedTransaction.createdAt
        )
    }

    suspend fun markParsedTransactionAsProcessed(id: String) {
        queries.updateParsedTransactionProcessed(id)
    }

    suspend fun deleteParsedTransaction(id: String) {
        queries.deleteParsedTransaction(id)
    }

    suspend fun deleteAllParsedTransactions() {
        queries.deleteAllParsedTransactions()
    }

    // Category 관련 메서드들
    fun getAllCategories(): Flow<List<Category>> {
        return queries.selectAllCategories()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toCategory() }
            }
    }

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return queries.selectCategoriesByType(type.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toCategory() }
            }
    }

    suspend fun getCategoryById(id: String): Category? {
        return queries.selectCategoryById(id).executeAsOneOrNull()?.toCategory()
    }

    suspend fun insertCategory(category: Category) {
        queries.insertCategory(
            id = category.id,
            name = category.name,
            emoji = category.emoji,
            type = category.type.name,
            isActive = if (category.isActive) 1L else 0L,
            sortOrder = category.sortOrder.toLong()
        )
    }

    suspend fun updateCategory(category: Category) {
        queries.updateCategory(
            name = category.name,
            emoji = category.emoji,
            type = category.type.name,
            isActive = if (category.isActive) 1L else 0L,
            sortOrder = category.sortOrder.toLong(),
            id = category.id
        )
    }

    suspend fun deleteCategory(id: String) {
        queries.deleteCategory(id)
    }

    suspend fun deleteAllCategories() {
        queries.deleteAllCategories()
    }

    // BudgetPlan 관련 메서드들
    fun getBudgetPlanByDate(date: LocalDate): Flow<BudgetPlan?> {
        return queries.selectBudgetPlanByDate(date.toString())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.firstOrNull()?.toBudgetPlan()
            }
    }

    fun getAllBudgetPlans(): Flow<List<BudgetPlan>> {
        return queries.selectAllBudgetPlans()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toBudgetPlan() }
            }
    }

    suspend fun insertBudgetPlan(budgetPlan: BudgetPlan) {
        queries.insertBudgetPlan(
            id = budgetPlan.id,
            effectiveFromDate = budgetPlan.effectiveFromDate.toString(),
            monthlySalary = budgetPlan.monthlySalary,
            createdAt = budgetPlan.createdAt.toString()
        )
    }

    suspend fun deleteBudgetPlan(id: String) {
        queries.deleteBudgetPlan(id)
    }

    suspend fun deleteAllBudgetPlans() {
        queries.deleteAllBudgetPlans()
    }

    // CategoryBudget 관련 메서드들
    fun getCategoryBudgetsByPlanId(budgetPlanId: String): Flow<List<CategoryBudget>> {
        return queries.selectCategoryBudgetsByPlanId(budgetPlanId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toCategoryBudget() }
            }
    }

    suspend fun insertCategoryBudget(categoryBudget: CategoryBudget) {
        queries.insertCategoryBudget(
            id = categoryBudget.id,
            budgetPlanId = categoryBudget.budgetPlanId,
            categoryIds = json.encodeToString(categoryBudget.categoryIds),
            categoryName = categoryBudget.categoryName,
            categoryEmoji = categoryBudget.categoryEmoji,
            allocatedAmount = categoryBudget.allocatedAmount,
            memo = categoryBudget.memo
        )
    }

    suspend fun updateCategoryBudget(id: String, allocatedAmount: Double, memo: String? = null) {
        queries.updateCategoryBudget(allocatedAmount, memo, id)
    }

    suspend fun deleteCategoryBudget(id: String) {
        queries.deleteCategoryBudget(id)
    }

    suspend fun deleteAllCategoryBudgets() {
        queries.deleteAllCategoryBudgets()
    }

    suspend fun getSpentAmountByCategory(categoryName: String, startDate: LocalDate, endDate: LocalDate): Double {
        return queries.selectSpentAmountByCategory(categoryName, startDate.toString(), endDate.toString())
            .executeAsOne().spent ?: 0.0
    }

    /**
     * 모든 데이터를 삭제합니다 (백업 복원 시 사용)
     */
    suspend fun deleteAllData() {
        // 외래 키 관계를 고려한 순서로 삭제
        queries.deleteAllCategoryBudgets()  // 예산 관련 먼저 삭제
        queries.deleteAllBudgetPlans()
        queries.deleteAllTransactions()
        queries.deleteAllBalanceCards()
        queries.deleteAllGiftCards()
        queries.deleteAllCategories()  // 카테고리는 마지막에 삭제
        // 파싱된 거래 내역은 유지 (백업 대상이 아님)
    }

    private fun TransactionEntity.toTransaction(): Transaction {
        return Transaction(
            id = this.id,
            amount = this.amount,
            type = TransactionType.valueOf(this.type),
            category = this.category,
            merchant = this.merchant,
            memo = this.memo,
            date = LocalDate.parse(this.date),
            incomeType = this.incomeType?.let { IncomeType.valueOf(it) },
            paymentMethod = this.paymentMethod?.let { PaymentMethod.valueOf(it) },
            balanceCardId = this.balanceCardId,
            giftCardId = this.giftCardId,
            cardName = this.cardName,
            actualAmount = this.actualAmount,
            settlementAmount = this.settlementAmount,
            isSettlement = this.isSettlement == 1L
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

    private fun ParsedTransactionEntity.toParsedTransaction(): ParsedTransaction {
        return ParsedTransaction(
            id = this.id,
            amount = this.amount,
            merchantName = this.merchantName,
            date = LocalDate.parse(this.date),
            rawNotification = this.rawNotification,
            isProcessed = this.isProcessed == 1L,
            createdAt = this.createdAt
        )
    }

    private fun CategoryEntity.toCategory(): Category {
        return Category(
            id = this.id,
            name = this.name,
            emoji = this.emoji,
            type = TransactionType.valueOf(this.type),
            isActive = this.isActive == 1L,
            sortOrder = this.sortOrder.toInt()
        )
    }

    private fun BudgetPlanEntity.toBudgetPlan(): BudgetPlan {
        return BudgetPlan(
            id = this.id,
            effectiveFromDate = LocalDate.parse(this.effectiveFromDate),
            monthlySalary = this.monthlySalary,
            createdAt = LocalDate.parse(this.createdAt)
        )
    }

    private fun CategoryBudgetEntity.toCategoryBudget(): CategoryBudget {
        val categoryIdsList = try {
            json.decodeFromString<List<String>>(this.categoryIds)
        } catch (e: Exception) {
            // 하위 호환성: 단일 ID가 저장되어 있을 경우 리스트로 변환
            listOf(this.categoryIds)
        }

        return CategoryBudget(
            id = this.id,
            budgetPlanId = this.budgetPlanId,
            categoryIds = categoryIdsList,
            categoryName = this.categoryName,
            categoryEmoji = this.categoryEmoji,
            allocatedAmount = this.allocatedAmount,
            memo = this.memo
        )
    }
}