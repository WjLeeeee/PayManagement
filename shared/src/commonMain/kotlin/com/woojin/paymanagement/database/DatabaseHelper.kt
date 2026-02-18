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
import com.woojin.paymanagement.data.FailedNotification
import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.BudgetPlan
import com.woojin.paymanagement.data.CategoryBudget
import com.woojin.paymanagement.data.RecurringTransaction
import com.woojin.paymanagement.data.RecurringPattern
import com.woojin.paymanagement.data.CustomPaymentMethod
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

    /**
     * merchant로 가장 많이 사용된 카테고리를 제안
     * 카드 파싱된 거래에서 자동 카테고리 선택에 사용
     */
    suspend fun getSuggestedCategory(merchant: String): String? {
        return try {
            queries.getSuggestedCategoryByMerchant(merchant).executeAsOneOrNull()?.category
        } catch (e: Exception) {
            null
        }
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

    suspend fun insertParsedTransaction(parsedTransaction: ParsedTransaction): Boolean {
        // INSERT OR IGNORE는 중복 시 무시하므로, 실제로 삽입되었는지 확인
        val existingTransaction = queries.selectParsedTransactionById(parsedTransaction.id).executeAsOneOrNull()

        if (existingTransaction != null) {
            // 이미 존재하는 경우 삽입 실패
            return false
        }

        queries.insertParsedTransaction(
            id = parsedTransaction.id,
            amount = parsedTransaction.amount,
            merchantName = parsedTransaction.merchantName,
            date = parsedTransaction.date.toString(),
            rawNotification = parsedTransaction.rawNotification,
            isProcessed = if (parsedTransaction.isProcessed) 1L else 0L,
            createdAt = parsedTransaction.createdAt
        )

        // 삽입 성공
        return true
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

    suspend fun hasRecentTransactionWithAmount(amount: Double, startTime: Long, endTime: Long): Boolean {
        return queries.checkRecentTransactionByAmount(amount, startTime, endTime).executeAsOne()
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

    // RecurringTransaction 관련 메서드들
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return queries.selectAllRecurringTransactions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toRecurringTransaction() }
            }
    }

    fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return queries.selectActiveRecurringTransactions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toRecurringTransaction() }
            }
    }

    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction) {
        queries.insertRecurringTransaction(
            id = recurringTransaction.id,
            type = recurringTransaction.type.name,
            category = recurringTransaction.category,
            amount = recurringTransaction.amount,
            merchant = recurringTransaction.merchant,
            memo = recurringTransaction.memo,
            paymentMethod = recurringTransaction.paymentMethod.name,
            balanceCardId = recurringTransaction.balanceCardId,
            giftCardId = recurringTransaction.giftCardId,
            cardName = recurringTransaction.cardName,
            pattern = recurringTransaction.pattern.name,
            dayOfMonth = recurringTransaction.dayOfMonth?.toLong(),
            dayOfWeek = recurringTransaction.dayOfWeek?.toLong(),
            weekendHandling = recurringTransaction.weekendHandling.name,
            isActive = if (recurringTransaction.isActive) 1 else 0,
            createdAt = recurringTransaction.createdAt,
            lastExecutedDate = recurringTransaction.lastExecutedDate
        )
    }

    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction) {
        queries.updateRecurringTransaction(
            type = recurringTransaction.type.name,
            category = recurringTransaction.category,
            amount = recurringTransaction.amount,
            merchant = recurringTransaction.merchant,
            memo = recurringTransaction.memo,
            paymentMethod = recurringTransaction.paymentMethod.name,
            balanceCardId = recurringTransaction.balanceCardId,
            giftCardId = recurringTransaction.giftCardId,
            cardName = recurringTransaction.cardName,
            pattern = recurringTransaction.pattern.name,
            dayOfMonth = recurringTransaction.dayOfMonth?.toLong(),
            dayOfWeek = recurringTransaction.dayOfWeek?.toLong(),
            weekendHandling = recurringTransaction.weekendHandling.name,
            isActive = if (recurringTransaction.isActive) 1 else 0,
            id = recurringTransaction.id
        )
    }

    suspend fun updateRecurringTransactionLastExecuted(id: String, date: String) {
        queries.updateRecurringTransactionLastExecuted(
            lastExecutedDate = date,
            id = id
        )
    }

    suspend fun deleteRecurringTransaction(id: String) {
        queries.deleteRecurringTransaction(id)
    }

    suspend fun deleteAllRecurringTransactions() {
        queries.deleteAllRecurringTransactions()
    }

    private fun RecurringTransactionEntity.toRecurringTransaction(): RecurringTransaction {
        return RecurringTransaction(
            id = this.id,
            type = TransactionType.valueOf(this.type),
            category = this.category,
            amount = this.amount,
            merchant = this.merchant,
            memo = this.memo,
            paymentMethod = PaymentMethod.valueOf(this.paymentMethod),
            balanceCardId = this.balanceCardId,
            giftCardId = this.giftCardId,
            cardName = this.cardName,
            pattern = RecurringPattern.valueOf(this.pattern),
            dayOfMonth = this.dayOfMonth?.toInt(),
            dayOfWeek = this.dayOfWeek?.toInt(),
            weekendHandling = try {
                com.woojin.paymanagement.data.WeekendHandling.valueOf(this.weekendHandling)
            } catch (e: Exception) {
                com.woojin.paymanagement.data.WeekendHandling.AS_IS
            },
            isActive = this.isActive == 1L,
            createdAt = this.createdAt,
            lastExecutedDate = this.lastExecutedDate
        )
    }

    // Holiday 관련 메서드들
    suspend fun insertHoliday(locdate: String, dateName: String, isHoliday: String, year: Long) {
        queries.insertHoliday(
            locdate = locdate,
            dateName = dateName,
            isHoliday = isHoliday,
            year = year
        )
    }

    suspend fun getHolidayByDate(date: String): HolidayEntity? {
        return queries.selectHolidayByDate(date).executeAsOneOrNull()
    }

    suspend fun getHolidaysByYear(year: Long): List<HolidayEntity> {
        return queries.selectHolidaysByYear(year).executeAsList()
    }

    suspend fun deleteHolidaysByYear(year: Long) {
        queries.deleteHolidaysByYear(year)
    }

    suspend fun getLatestHolidayDate(): String? {
        return queries.selectLatestHolidayDate().executeAsOneOrNull()
    }

    // FailedNotification 관련 메서드들
    suspend fun insertFailedNotification(failedNotification: FailedNotification) {
        queries.insertFailedNotification(
            packageName = failedNotification.packageName,
            title = failedNotification.title,
            text = failedNotification.text,
            bigText = failedNotification.bigText,
            failureReason = failedNotification.failureReason,
            createdAt = failedNotification.createdAt
        )
    }

    fun getAllFailedNotifications(): Flow<List<FailedNotification>> {
        return queries.selectAllFailedNotifications()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toFailedNotification() }
            }
    }

    suspend fun getRecentFailedNotifications(limit: Long): List<FailedNotification> {
        return queries.selectRecentFailedNotifications(limit)
            .executeAsList()
            .map { it.toFailedNotification() }
    }

    suspend fun deleteFailedNotification(id: Long) {
        queries.deleteFailedNotification(id)
    }

    suspend fun deleteAllFailedNotifications() {
        queries.deleteAllFailedNotifications()
    }

    suspend fun deleteOldFailedNotifications(beforeTimestamp: Long) {
        queries.deleteOldFailedNotifications(beforeTimestamp)
    }

    private fun FailedNotificationEntity.toFailedNotification(): FailedNotification {
        return FailedNotification(
            id = this.id,
            packageName = this.packageName,
            title = this.title,
            text = this.text,
            bigText = this.bigText,
            failureReason = this.failureReason,
            createdAt = this.createdAt
        )
    }

    // CustomPaymentMethod 관련 메서드들
    fun getAllCustomPaymentMethods(): Flow<List<CustomPaymentMethod>> {
        return queries.selectAllCustomPaymentMethods()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toCustomPaymentMethod() }
            }
    }

    fun getAllCustomPaymentMethodsIncludingInactive(): Flow<List<CustomPaymentMethod>> {
        return queries.selectAllCustomPaymentMethodsIncludingInactive()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toCustomPaymentMethod() }
            }
    }

    suspend fun insertCustomPaymentMethod(customPaymentMethod: CustomPaymentMethod) {
        queries.insertCustomPaymentMethod(
            id = customPaymentMethod.id,
            name = customPaymentMethod.name,
            isActive = if (customPaymentMethod.isActive) 1L else 0L,
            sortOrder = customPaymentMethod.sortOrder.toLong(),
            isDefault = if (customPaymentMethod.isDefault) 1L else 0L
        )
    }

    suspend fun updateCustomPaymentMethod(customPaymentMethod: CustomPaymentMethod) {
        queries.updateCustomPaymentMethod(
            name = customPaymentMethod.name,
            isActive = if (customPaymentMethod.isActive) 1L else 0L,
            sortOrder = customPaymentMethod.sortOrder.toLong(),
            isDefault = if (customPaymentMethod.isDefault) 1L else 0L,
            id = customPaymentMethod.id
        )
    }

    suspend fun clearAllDefaultPaymentMethods() {
        queries.clearAllDefaultPaymentMethods()
    }

    suspend fun deleteCustomPaymentMethod(id: String) {
        queries.deleteCustomPaymentMethod(id)
    }

    suspend fun deleteAllCustomPaymentMethods() {
        queries.deleteAllCustomPaymentMethods()
    }

    suspend fun updateTransactionsCardName(oldCardName: String, newCardName: String) {
        queries.updateTransactionsCardName(newCardName, oldCardName)
    }

    private fun CustomPaymentMethodEntity.toCustomPaymentMethod(): CustomPaymentMethod {
        return CustomPaymentMethod(
            id = this.id,
            name = this.name,
            isActive = this.isActive == 1L,
            sortOrder = this.sortOrder.toInt(),
            isDefault = this.isDefault == 1L
        )
    }
}