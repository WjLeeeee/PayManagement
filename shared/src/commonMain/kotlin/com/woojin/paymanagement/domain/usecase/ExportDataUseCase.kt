package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.BackupData
import com.woojin.paymanagement.data.BalanceCardBackup
import com.woojin.paymanagement.data.GiftCardBackup
import com.woojin.paymanagement.data.TransactionBackup
import com.woojin.paymanagement.data.CategoryBackup
import com.woojin.paymanagement.data.BudgetPlanBackup
import com.woojin.paymanagement.data.CategoryBudgetBackup
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.model.BackupType
import com.woojin.paymanagement.utils.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 데이터를 JSON 형식으로 내보내는 UseCase
 */
class ExportDataUseCase(
    private val databaseHelper: DatabaseHelper,
    private val preferencesManager: PreferencesManager
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend operator fun invoke(type: BackupType = BackupType.ALL): Result<String> {
        return try {
            // 타입에 따라 필요한 데이터만 수집
            val transactions = if (type == BackupType.ALL || type == BackupType.TRANSACTIONS) {
                databaseHelper.getAllTransactions().first()
            } else emptyList()

            val balanceCards = if (type == BackupType.ALL || type == BackupType.CARDS) {
                databaseHelper.getAllBalanceCards().first()
            } else emptyList()

            val giftCards = if (type == BackupType.ALL || type == BackupType.CARDS) {
                databaseHelper.getAllGiftCards().first()
            } else emptyList()

            val categories = if (type == BackupType.ALL || type == BackupType.CATEGORIES) {
                databaseHelper.getAllCategories().first()
            } else emptyList()

            val budgetPlans = if (type == BackupType.ALL || type == BackupType.BUDGET) {
                databaseHelper.getAllBudgetPlans().first()
            } else emptyList()

            // 예산 계획에 대한 카테고리 예산 수집
            val allCategoryBudgets = if (type == BackupType.ALL || type == BackupType.BUDGET) {
                budgetPlans.flatMap { plan ->
                    databaseHelper.getCategoryBudgetsByPlanId(plan.id).first()
                }
            } else emptyList()

            // 백업 데이터 생성
            val backupData = BackupData(
                version = 3,
                exportDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
                payday = preferencesManager.getPayday(),
                paydayAdjustment = preferencesManager.getPaydayAdjustment().name,
                transactions = transactions.map { it.toBackup() },
                balanceCards = balanceCards.map { it.toBackup() },
                giftCards = giftCards.map { it.toBackup() },
                categories = categories.map { it.toBackup() },
                budgetPlans = budgetPlans.map { it.toBackup() },
                categoryBudgets = allCategoryBudgets.map { it.toBackup() }
            )

            // JSON 변환
            val jsonString = json.encodeToString(backupData)
            Result.success(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun com.woojin.paymanagement.data.Transaction.toBackup() = TransactionBackup(
        id = id,
        date = date.toString(),
        amount = amount,
        type = type.name,
        category = category,
        memo = memo,
        paymentMethod = paymentMethod?.name,
        incomeType = incomeType?.name,
        balanceCardId = balanceCardId,
        giftCardId = giftCardId,
        cardName = cardName,
        merchant = merchant,
        actualAmount = actualAmount,
        settlementAmount = settlementAmount,
        isSettlement = isSettlement
    )

    private fun com.woojin.paymanagement.data.BalanceCard.toBackup() = BalanceCardBackup(
        id = id,
        name = name,
        initialAmount = initialAmount,
        currentBalance = currentBalance,
        createdDate = createdDate.toString(),
        isActive = isActive
    )

    private fun com.woojin.paymanagement.data.GiftCard.toBackup() = GiftCardBackup(
        id = id,
        name = name,
        totalAmount = totalAmount,
        usedAmount = usedAmount,
        createdDate = createdDate.toString(),
        isActive = isActive,
        minimumUsageRate = minimumUsageRate
    )

    private fun com.woojin.paymanagement.data.Category.toBackup() = CategoryBackup(
        id = id,
        name = name,
        emoji = emoji,
        type = type.name,
        isActive = isActive,
        sortOrder = sortOrder
    )

    private fun com.woojin.paymanagement.data.BudgetPlan.toBackup() = BudgetPlanBackup(
        id = id,
        periodStartDate = periodStartDate.toString(),
        periodEndDate = periodEndDate.toString(),
        createdAt = createdAt.toString()
    )

    private fun com.woojin.paymanagement.data.CategoryBudget.toBackup() = CategoryBudgetBackup(
        id = id,
        budgetPlanId = budgetPlanId,
        categoryIds = categoryIds,
        categoryName = categoryName,
        categoryEmoji = categoryEmoji,
        allocatedAmount = allocatedAmount,
        memo = memo
    )
}
