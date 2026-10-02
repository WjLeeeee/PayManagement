package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * 특정 급여 기간에 이체 완료 체크된 TransferItem id 집합
 */
class GetTransferChecksUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(periodStartDate: LocalDate): Flow<Set<String>> {
        return repository.getTransferChecksByPeriod(periodStartDate)
    }
}
