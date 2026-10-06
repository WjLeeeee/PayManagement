package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.BudgetRepository
import kotlinx.datetime.LocalDate

/**
 * 특정 급여 기간의 이체 완료 체크 설정/해제
 */
class SetTransferCheckedUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(periodStartDate: LocalDate, transferItemId: String, checked: Boolean) {
        repository.setTransferChecked(periodStartDate, transferItemId, checked)
    }
}
