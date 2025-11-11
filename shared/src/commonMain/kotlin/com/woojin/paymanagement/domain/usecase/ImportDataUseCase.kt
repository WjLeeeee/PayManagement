package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.*
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.utils.PreferencesManager
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate

/**
 * JSON 형식의 데이터를 가져와서 복원하는 UseCase
 */
class ImportDataUseCase(
    private val databaseHelper: DatabaseHelper,
    private val preferencesManager: PreferencesManager
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend operator fun invoke(jsonString: String, replaceExisting: Boolean = false): Result<ImportResult> {
        return try {
            // JSON 파싱
            val backupData = json.decodeFromString<BackupData>(jsonString)

            // 기존 데이터 삭제 (선택적)
            if (replaceExisting) {
                databaseHelper.deleteAllData()
            }

            // 설정 복원
            preferencesManager.setPayday(backupData.payday)
            preferencesManager.setPaydayAdjustment(
                com.woojin.paymanagement.utils.PaydayAdjustment.valueOf(backupData.paydayAdjustment)
            )

            // 데이터 복원
            var successCount = 0
            var failureCount = 0

            // 카테고리 복원 (먼저 복원 - 다른 데이터가 의존할 수 있음)
            backupData.categories.forEach { backup ->
                try {
                    val category = backup.toCategory()
                    databaseHelper.insertCategory(category)
                    successCount++
                } catch (e: Exception) {
                    failureCount++
                }
            }

            // 잔액권 복원
            backupData.balanceCards.forEach { backup ->
                try {
                    val balanceCard = backup.toBalanceCard()
                    databaseHelper.insertBalanceCard(balanceCard)
                    successCount++
                } catch (e: Exception) {
                    failureCount++
                }
            }

            // 상품권 복원
            backupData.giftCards.forEach { backup ->
                try {
                    val giftCard = backup.toGiftCard()
                    databaseHelper.insertGiftCard(giftCard)
                    successCount++
                } catch (e: Exception) {
                    failureCount++
                }
            }

            // 예산 계획 복원
            backupData.budgetPlans.forEach { backup ->
                try {
                    val budgetPlan = backup.toBudgetPlan()
                    databaseHelper.insertBudgetPlan(budgetPlan)
                    successCount++
                } catch (e: Exception) {
                    failureCount++
                }
            }

            // 카테고리 예산 복원 (예산 계획 이후에 복원)
            backupData.categoryBudgets.forEach { backup ->
                try {
                    val categoryBudget = backup.toCategoryBudget()
                    databaseHelper.insertCategoryBudget(categoryBudget)
                    successCount++
                } catch (e: Exception) {
                    failureCount++
                }
            }

            // 거래 내역 복원
            backupData.transactions.forEach { backup ->
                try {
                    val transaction = backup.toTransaction()
                    databaseHelper.insertTransaction(transaction)
                    successCount++
                } catch (e: Exception) {
                    failureCount++
                }
            }

            Result.success(
                ImportResult(
                    totalCount = backupData.transactions.size + backupData.balanceCards.size +
                                 backupData.giftCards.size + backupData.categories.size +
                                 backupData.budgetPlans.size + backupData.categoryBudgets.size,
                    successCount = successCount,
                    failureCount = failureCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun TransactionBackup.toTransaction() = Transaction(
        id = id,
        date = LocalDate.parse(date),
        amount = amount,
        type = TransactionType.valueOf(type),
        category = category ?: "",
        memo = memo ?: "",
        paymentMethod = paymentMethod?.let { PaymentMethod.valueOf(it) },
        incomeType = incomeType?.let { IncomeType.valueOf(it) },
        balanceCardId = balanceCardId,
        giftCardId = giftCardId,
        cardName = cardName,
        merchant = merchant,
        actualAmount = actualAmount,
        settlementAmount = settlementAmount,
        isSettlement = isSettlement
    )

    private fun BalanceCardBackup.toBalanceCard() = BalanceCard(
        id = id,
        name = name,
        initialAmount = initialAmount,
        currentBalance = currentBalance,
        createdDate = LocalDate.parse(createdDate),
        isActive = isActive
    )

    private fun GiftCardBackup.toGiftCard() = GiftCard(
        id = id,
        name = name,
        totalAmount = totalAmount,
        usedAmount = usedAmount,
        createdDate = LocalDate.parse(createdDate),
        isActive = isActive,
        minimumUsageRate = minimumUsageRate
    )

    private fun CategoryBackup.toCategory() = Category(
        id = id,
        name = name,
        emoji = emoji,
        type = TransactionType.valueOf(type),
        isActive = isActive,
        sortOrder = sortOrder
    )

    private fun BudgetPlanBackup.toBudgetPlan() = BudgetPlan(
        id = id,
        periodStartDate = LocalDate.parse(periodStartDate),
        periodEndDate = LocalDate.parse(periodEndDate),
        createdAt = LocalDate.parse(createdAt)
    )

    private fun CategoryBudgetBackup.toCategoryBudget() = CategoryBudget(
        id = id,
        budgetPlanId = budgetPlanId,
        categoryIds = categoryIds,
        categoryName = categoryName,
        categoryEmoji = categoryEmoji,
        allocatedAmount = allocatedAmount,
        memo = memo
    )
}

data class ImportResult(
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int
)
