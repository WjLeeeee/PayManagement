package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.CategoryBudget
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.PayPeriodCalculator
import com.woojin.paymanagement.utils.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/**
 * 예산 초과 여부를 체크하는 UseCase
 */
class CheckBudgetExceededUseCase(
    private val databaseHelper: DatabaseHelper,
    private val payPeriodCalculator: PayPeriodCalculator,
    private val preferencesManager: PreferencesManager
) {

    /**
     * 거래 추가 후 예산 체크
     *
     * @param transaction 추가된 거래
     * @return 초과된 임계값과 카테고리 예산 정보 (초과하지 않았으면 null)
     */
    suspend operator fun invoke(transaction: Transaction): BudgetExceededResult? {
        // 지출 거래가 아니면 체크하지 않음
        if (transaction.type != TransactionType.EXPENSE) {
            return null
        }

        // 현재 적용 중인 예산 플랜 가져오기
        val currentBudgetPlan = databaseHelper.getBudgetPlanByDate(transaction.date).first()
        if (currentBudgetPlan == null) {
            return null
        }

        // 거래의 카테고리 이름으로 카테고리 ID 찾기
        val allCategories = databaseHelper.getAllCategories().first()
        val transactionCategoryId = allCategories.find { it.name == transaction.category }?.id
        if (transactionCategoryId == null) {
            return null
        }

        // 해당 플랜의 모든 카테고리 예산 가져오기
        val categoryBudgets = databaseHelper.getCategoryBudgetsByPlanId(currentBudgetPlan.id).first()

        // 현재 거래가 속한 카테고리 예산들 찾기
        val relevantBudgets = categoryBudgets.filter { budget ->
            budget.categoryIds.contains(transactionCategoryId)
        }

        if (relevantBudgets.isEmpty()) {
            return null
        }

        // 급여 기간 계산
        val payday = preferencesManager.getPayday()
        val adjustment = preferencesManager.getPaydayAdjustment()
        val payPeriod = payPeriodCalculator.getCurrentPayPeriod(payday, adjustment, transaction.date)

        // 전체 거래 목록 가져오기
        val allTransactions = databaseHelper.getAllTransactions().first()

        // 각 관련 예산에 대해 체크 (가장 먼저 초과한 예산 하나만 반환)
        for (budget in relevantBudgets) {
            val result = checkSingleBudget(budget, allTransactions, payPeriod, allCategories)
            if (result != null) return result
        }

        return null
    }

    /**
     * 단일 예산 체크
     */
    private fun checkSingleBudget(
        budget: CategoryBudget,
        allTransactions: List<Transaction>,
        payPeriod: PayPeriod,
        allCategories: List<com.woojin.paymanagement.data.Category>
    ): BudgetExceededResult? {
        // 현재 사용량 계산
        val (usedAmount, usageRate) = calculateUsage(budget, allTransactions, payPeriod, allCategories)

        // 임계값 체크 (70%, 100%)
        val thresholds = listOf(100, 70)  // 높은 순서로 체크
        for (threshold in thresholds) {
            val thresholdRate = threshold / 100.0
            // "초과" 체크: > (이상이 아님!)
            if (usageRate > thresholdRate) {
                return BudgetExceededResult(
                    categoryBudget = budget,
                    usedAmount = usedAmount,
                    budgetAmount = budget.allocatedAmount,
                    usageRate = usageRate,
                    threshold = threshold
                )
            }
        }

        return null
    }

    /**
     * 카테고리 예산의 현재 사용량 계산
     */
    private fun calculateUsage(
        categoryBudget: CategoryBudget,
        transactions: List<Transaction>,
        payPeriod: PayPeriod,
        allCategories: List<com.woojin.paymanagement.data.Category>
    ): Pair<Double, Double> {
        // 현재 급여 기간 내 지출 거래만 필터링
        val periodTransactions = transactions.filter { transaction ->
            transaction.date >= payPeriod.startDate &&
            transaction.date <= payPeriod.endDate &&
            transaction.type == TransactionType.EXPENSE
        }

        // 카테고리 이름 -> ID 맵핑
        val categoryNameToId = allCategories.associate { it.name to it.id }

        // categoryBudget의 categoryIds에 해당하는 거래들만 필터링
        val relevantTransactions = periodTransactions.filter { transaction ->
            val transactionCategoryId = categoryNameToId[transaction.category]
            transactionCategoryId != null && categoryBudget.categoryIds.contains(transactionCategoryId)
        }

        // 총 사용 금액 (displayAmount 사용 - 정산금액 제외)
        val usedAmount = relevantTransactions.sumOf { it.displayAmount }

        // 사용률 (1.0 = 100%)
        val usageRate = if (categoryBudget.allocatedAmount > 0) {
            usedAmount / categoryBudget.allocatedAmount
        } else {
            0.0
        }

        return Pair(usedAmount, usageRate)
    }
}

/**
 * 예산 초과 결과
 */
data class BudgetExceededResult(
    val categoryBudget: CategoryBudget,
    val usedAmount: Double,
    val budgetAmount: Double,
    val usageRate: Double,
    val threshold: Int  // 70 or 100
)
