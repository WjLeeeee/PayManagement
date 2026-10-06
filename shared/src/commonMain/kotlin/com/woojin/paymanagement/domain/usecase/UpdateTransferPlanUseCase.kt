package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.TransferItem
import com.woojin.paymanagement.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.first

/**
 * 월급날 이체 계획 저장
 * 계획에서 사라진 통장을 참조하던 예산은 해당 통장 연결만 제거한다
 */
class UpdateTransferPlanUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(
        budgetPlanId: String,
        transfers: List<TransferItem>,
        previousTransfers: List<TransferItem>
    ) {
        repository.updateBudgetPlanTransfers(budgetPlanId, transfers)

        val remainingIds = transfers.map { it.id }.toSet()
        val removedIds = previousTransfers.map { it.id }.filter { it !in remainingIds }.toSet()
        if (removedIds.isEmpty()) return

        repository.getCategoryBudgetsByPlanId(budgetPlanId).first()
            .filter { budget -> budget.transferItemIds.any { it in removedIds } }
            .forEach { budget ->
                val remaining = budget.accountAllocations.filter { it.transferItemId !in removedIds }
                repository.updateCategoryBudget(
                    id = budget.id,
                    allocatedAmount = budget.allocatedAmount,
                    memo = budget.memo,
                    items = budget.items,
                    // 통장이 하나만 남으면 예산 전액으로 간주하므로 금액은 비움
                    accountAllocations = if (remaining.size == 1) remaining.map { it.copy(amount = null) } else remaining
                )
            }
    }
}
