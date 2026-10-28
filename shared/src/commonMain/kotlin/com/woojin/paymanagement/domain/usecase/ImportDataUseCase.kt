package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.*
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.model.BackupType
import com.woojin.paymanagement.domain.repository.PreferencesRepository
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate

/**
 * JSON 형식의 데이터를 가져와서 복원하는 UseCase
 */
class ImportDataUseCase(
    private val databaseHelper: DatabaseHelper,
    private val preferencesRepository: PreferencesRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend operator fun invoke(
        jsonString: String,
        replaceExisting: Boolean = false
    ): Result<ImportResult> {
        return try {
            // JSON 문자열 검증
            if (jsonString.isBlank()) {
                return Result.failure(Exception("JSON 파일이 비어있습니다"))
            }

            // JSON 파싱
            val backupData = try {
                json.decodeFromString<BackupData>(jsonString)
            } catch (e: Exception) {
                return Result.failure(Exception("JSON 파싱 실패: ${e.message}\n파일 형식을 확인해주세요"))
            }

            // 1) 모든 항목을 먼저 변환. 하나라도 못 읽으면 기존 데이터를 건드리지 않고 중단
            val restore = try {
                RestoreData(
                    categories = backupData.categories.map { it.toCategory() },
                    balanceCards = backupData.balanceCards.map { it.toBalanceCard() },
                    budgetPlans = backupData.budgetPlans.map { it.toBudgetPlan() },
                    categoryBudgets = backupData.categoryBudgets.map { it.toCategoryBudget() },
                    // 이체 완료 체크는 보조 데이터라 못 읽는 항목은 건너뜀 (기존 동작 유지)
                    transferChecks = backupData.transferChecks.mapNotNull { backup ->
                        runCatching { LocalDate.parse(backup.periodStartDate) to backup.transferItemId }.getOrNull()
                    },
                    transactions = backupData.transactions.map { it.toTransaction() },
                    recurringTransactions = backupData.recurringTransactions.map { it.toRecurringTransaction() },
                    customPaymentMethods = backupData.customPaymentMethods.map { it.toCustomPaymentMethod() },
                    paydayAdjustment = if (backupData.payday > 0) {
                        com.woojin.paymanagement.utils.PaydayAdjustment.valueOf(backupData.paydayAdjustment)
                    } else null
                )
            } catch (e: Exception) {
                return Result.failure(Exception("백업 파일에 읽을 수 없는 항목이 있어 가져오지 않았습니다. 기존 데이터는 그대로입니다.\n${e.message}"))
            }

            // JSON에 실제로 포함된 데이터 타입 감지
            val hasCategories = restore.categories.isNotEmpty()
            val hasCards = restore.balanceCards.isNotEmpty()
            val hasBudget = restore.budgetPlans.isNotEmpty() || restore.categoryBudgets.isNotEmpty()
            val hasTransactions = restore.transactions.isNotEmpty()
            val hasRecurringTransactions = restore.recurringTransactions.isNotEmpty()
            val hasCustomPaymentMethods = restore.customPaymentMethods.isNotEmpty()

            // 2) 삭제 + 삽입을 한 트랜잭션으로. 중간에 실패하거나 앱이 꺼지면 전부 되돌아감
            databaseHelper.transaction {
                // 기존 데이터 삭제 (선택적) - 실제로 가져올 데이터만 삭제
                if (replaceExisting) {
                    if (hasCategories) databaseHelper.deleteAllCategories()
                    if (hasCards) databaseHelper.deleteAllBalanceCards()
                    if (hasBudget) {
                        databaseHelper.deleteAllCategoryBudgets()
                        databaseHelper.deleteAllBudgetPlans()
                        databaseHelper.deleteAllTransferChecks()
                    }
                    if (hasTransactions) databaseHelper.deleteAllTransactions()
                    if (hasRecurringTransactions) databaseHelper.deleteAllRecurringTransactions()
                    if (hasCustomPaymentMethods) databaseHelper.deleteAllCustomPaymentMethods()
                }

                restore.categories.forEach { databaseHelper.insertCategory(it) }
                restore.balanceCards.forEach { databaseHelper.insertBalanceCard(it) }
                restore.budgetPlans.forEach { databaseHelper.insertBudgetPlan(it) }
                restore.categoryBudgets.forEach { databaseHelper.insertCategoryBudget(it) }
                // 이체 완료 체크 복원 (예산이 있을 때만, INSERT OR IGNORE라 중복은 무시됨)
                if (hasBudget) {
                    restore.transferChecks.forEach { (periodStartDate, transferItemId) ->
                        databaseHelper.setTransferChecked(periodStartDate, transferItemId, checked = true)
                    }
                }
                restore.transactions.forEach { databaseHelper.insertTransaction(it) }
                restore.recurringTransactions.forEach { databaseHelper.insertRecurringTransaction(it) }
                restore.customPaymentMethods.forEach { databaseHelper.insertCustomPaymentMethod(it) }
            }

            // 3) 설정 복원 (설정 정보가 있는 경우만, DB 복원이 성공한 뒤에)
            restore.paydayAdjustment?.let { adjustment ->
                preferencesRepository.setPayday(backupData.payday)
                preferencesRepository.setPaydayAdjustment(adjustment)
            }

            val totalCount = restore.categories.size + restore.balanceCards.size +
                restore.budgetPlans.size + restore.categoryBudgets.size + restore.transactions.size +
                restore.recurringTransactions.size + restore.customPaymentMethods.size
            Result.success(
                ImportResult(
                    totalCount = totalCount,
                    successCount = totalCount,
                    failureCount = 0
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 복원 전에 미리 변환해 둔 백업 내용 */
    private class RestoreData(
        val categories: List<Category>,
        val balanceCards: List<BalanceCard>,
        val budgetPlans: List<BudgetPlan>,
        val categoryBudgets: List<CategoryBudget>,
        val transferChecks: List<Pair<LocalDate, String>>,
        val transactions: List<Transaction>,
        val recurringTransactions: List<RecurringTransaction>,
        val customPaymentMethods: List<CustomPaymentMethod>,
        val paydayAdjustment: com.woojin.paymanagement.utils.PaydayAdjustment?
    )

    private fun resolveTransactionType(type: String, category: String?): TransactionType {
        val parsed = TransactionType.valueOf(type)
        val investmentCategories = setOf("투자", "손절", "익절", "배당금")
        return if (category in investmentCategories &&
            (parsed == TransactionType.EXPENSE || parsed == TransactionType.INCOME)) {
            TransactionType.INVESTMENT
        } else {
            parsed
        }
    }

    private fun TransactionBackup.toTransaction() = Transaction(
        id = id,
        date = LocalDate.parse(date),
        amount = amount,
        type = resolveTransactionType(type, category),
        category = category ?: "",
        memo = memo ?: "",
        paymentMethod = paymentMethod?.let { paymentMethodOf(it) },
        incomeType = incomeType?.let { incomeTypeOf(it) },
        balanceCardId = balanceCardId,
        cardName = cardName,
        merchant = merchant,
        settlementAmount = settlementAmount,
        isSettlement = isSettlement,
        subCategory = subCategory
    )

    private fun BalanceCardBackup.toBalanceCard() = BalanceCard(
        id = id,
        name = name,
        initialAmount = initialAmount,
        currentBalance = currentBalance,
        createdDate = LocalDate.parse(createdDate),
        isActive = isActive
    )

    private fun CategoryBackup.toCategory() = Category(
        id = id,
        name = name,
        emoji = emoji,
        type = resolveTransactionType(type, name),
        isActive = isActive,
        sortOrder = sortOrder,
        parentId = parentId
    )

    private fun BudgetPlanBackup.toBudgetPlan(): BudgetPlan {
        // v4 형식: effectiveFromDate와 monthlySalary 사용
        // v3 형식 (하위 호환): periodStartDate를 effectiveFromDate로 사용, monthlySalary는 0
        val effectiveDate = if (effectiveFromDate.isNotEmpty()) {
            LocalDate.parse(effectiveFromDate)
        } else {
            // v3 백업 데이터: periodStartDate를 effectiveFromDate로 변환
            LocalDate.parse(periodStartDate ?: throw IllegalArgumentException("Invalid budget plan backup data"))
        }

        return BudgetPlan(
            id = id,
            effectiveFromDate = effectiveDate,
            monthlySalary = monthlySalary,  // v3 데이터의 경우 0.0
            createdAt = LocalDate.parse(createdAt),
            transfers = transfers.map { TransferItem(id = it.id, accountName = it.accountName, amount = it.amount) }
        )
    }

    private fun CategoryBudgetBackup.toCategoryBudget() = CategoryBudget(
        id = id,
        budgetPlanId = budgetPlanId,
        categoryIds = categoryIds,
        categoryName = categoryName,
        categoryEmoji = categoryEmoji,
        allocatedAmount = allocatedAmount,
        memo = memo,
        items = items.map { BudgetItem(id = it.id, name = it.name, amount = it.amount) },
        accountAllocations = accountAllocations.map { BudgetAccountAllocation(transferItemId = it.transferItemId, amount = it.amount) }
    )

    private fun CustomPaymentMethodBackup.toCustomPaymentMethod() = CustomPaymentMethod(
        id = id,
        name = name,
        isActive = isActive,
        sortOrder = sortOrder,
        isDefault = isDefault
    )

    private fun RecurringTransactionBackup.toRecurringTransaction() = RecurringTransaction(
        id = id,
        type = resolveTransactionType(type, category),
        category = category,
        amount = amount,
        merchant = merchant,
        memo = memo,
        paymentMethod = paymentMethodOf(paymentMethod),
        balanceCardId = balanceCardId,
        cardName = cardName,
        pattern = RecurringPattern.valueOf(pattern),
        dayOfMonth = dayOfMonth,
        dayOfWeek = dayOfWeek,
        weekendHandling = WeekendHandling.valueOf(weekendHandling),
        includeWeekends = includeWeekends,
        isActive = isActive,
        createdAt = createdAt,
        lastExecutedDate = lastExecutedDate
    )
}

data class ImportResult(
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int
)
