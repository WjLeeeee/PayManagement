package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.*
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.model.BackupType
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

            // JSON에 실제로 포함된 데이터 타입 감지
            val hasCategories = backupData.categories.isNotEmpty()
            val hasCards = backupData.balanceCards.isNotEmpty() || backupData.giftCards.isNotEmpty()
            val hasBudget = backupData.budgetPlans.isNotEmpty() || backupData.categoryBudgets.isNotEmpty()
            val hasTransactions = backupData.transactions.isNotEmpty()
            val hasRecurringTransactions = backupData.recurringTransactions.isNotEmpty()
            val hasCustomPaymentMethods = backupData.customPaymentMethods.isNotEmpty()
            val hasSettings = backupData.payday > 0  // 설정 정보가 있는지 확인

            // 기존 데이터 삭제 (선택적) - 실제로 가져올 데이터만 삭제
            if (replaceExisting) {
                if (hasCategories) databaseHelper.deleteAllCategories()
                if (hasCards) {
                    databaseHelper.deleteAllBalanceCards()
                    databaseHelper.deleteAllGiftCards()
                }
                if (hasBudget) {
                    databaseHelper.deleteAllCategoryBudgets()
                    databaseHelper.deleteAllBudgetPlans()
                }
                if (hasTransactions) databaseHelper.deleteAllTransactions()
                if (hasRecurringTransactions) databaseHelper.deleteAllRecurringTransactions()
                if (hasCustomPaymentMethods) databaseHelper.deleteAllCustomPaymentMethods()
            }

            // 설정 복원 (설정 정보가 있는 경우만)
            if (hasSettings) {
                preferencesManager.setPayday(backupData.payday)
                preferencesManager.setPaydayAdjustment(
                    com.woojin.paymanagement.utils.PaydayAdjustment.valueOf(backupData.paydayAdjustment)
                )
            }

            // 데이터 복원
            var successCount = 0
            var failureCount = 0
            var totalCount = 0

            // 카테고리 복원 (데이터가 있는 경우만)
            if (hasCategories) {
                totalCount += backupData.categories.size
                backupData.categories.forEach { backup ->
                    try {
                        val category = backup.toCategory()
                        databaseHelper.insertCategory(category)
                        successCount++
                    } catch (e: Exception) {
                        failureCount++
                    }
                }
            }

            // 잔액권/상품권 복원 (데이터가 있는 경우만)
            if (hasCards) {
                totalCount += backupData.balanceCards.size + backupData.giftCards.size

                backupData.balanceCards.forEach { backup ->
                    try {
                        val balanceCard = backup.toBalanceCard()
                        databaseHelper.insertBalanceCard(balanceCard)
                        successCount++
                    } catch (e: Exception) {
                        failureCount++
                    }
                }

                backupData.giftCards.forEach { backup ->
                    try {
                        val giftCard = backup.toGiftCard()
                        databaseHelper.insertGiftCard(giftCard)
                        successCount++
                    } catch (e: Exception) {
                        failureCount++
                    }
                }
            }

            // 예산 설정 복원 (데이터가 있는 경우만)
            if (hasBudget) {
                totalCount += backupData.budgetPlans.size + backupData.categoryBudgets.size

                backupData.budgetPlans.forEach { backup ->
                    try {
                        val budgetPlan = backup.toBudgetPlan()
                        databaseHelper.insertBudgetPlan(budgetPlan)
                        successCount++
                    } catch (e: Exception) {
                        failureCount++
                    }
                }

                backupData.categoryBudgets.forEach { backup ->
                    try {
                        val categoryBudget = backup.toCategoryBudget()
                        databaseHelper.insertCategoryBudget(categoryBudget)
                        successCount++
                    } catch (e: Exception) {
                        failureCount++
                    }
                }
            }

            // 거래 내역 복원 (데이터가 있는 경우만)
            if (hasTransactions) {
                totalCount += backupData.transactions.size
                backupData.transactions.forEach { backup ->
                    try {
                        val transaction = backup.toTransaction()
                        databaseHelper.insertTransaction(transaction)
                        successCount++
                    } catch (e: Exception) {
                        failureCount++
                    }
                }
            }

            // 반복거래 복원 (데이터가 있는 경우만)
            if (hasRecurringTransactions) {
                totalCount += backupData.recurringTransactions.size
                backupData.recurringTransactions.forEach { backup ->
                    try {
                        val recurringTransaction = backup.toRecurringTransaction()
                        databaseHelper.insertRecurringTransaction(recurringTransaction)
                        successCount++
                    } catch (e: Exception) {
                        failureCount++
                    }
                }
            }

            // 커스텀 결제수단 복원 (데이터가 있는 경우만)
            if (hasCustomPaymentMethods) {
                totalCount += backupData.customPaymentMethods.size
                backupData.customPaymentMethods.forEach { backup ->
                    try {
                        val customPaymentMethod = backup.toCustomPaymentMethod()
                        databaseHelper.insertCustomPaymentMethod(customPaymentMethod)
                        successCount++
                    } catch (e: Exception) {
                        failureCount++
                    }
                }
            }

            Result.success(
                ImportResult(
                    totalCount = totalCount,
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
            createdAt = LocalDate.parse(createdAt)
        )
    }

    private fun CategoryBudgetBackup.toCategoryBudget() = CategoryBudget(
        id = id,
        budgetPlanId = budgetPlanId,
        categoryIds = categoryIds,
        categoryName = categoryName,
        categoryEmoji = categoryEmoji,
        allocatedAmount = allocatedAmount,
        memo = memo
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
        type = TransactionType.valueOf(type),
        category = category,
        amount = amount,
        merchant = merchant,
        memo = memo,
        paymentMethod = PaymentMethod.valueOf(paymentMethod),
        balanceCardId = balanceCardId,
        giftCardId = giftCardId,
        cardName = cardName,
        pattern = RecurringPattern.valueOf(pattern),
        dayOfMonth = dayOfMonth,
        dayOfWeek = dayOfWeek,
        weekendHandling = WeekendHandling.valueOf(weekendHandling),
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
