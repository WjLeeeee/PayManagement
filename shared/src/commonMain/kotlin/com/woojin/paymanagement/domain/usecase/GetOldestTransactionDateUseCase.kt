package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.TransactionRepository
import kotlinx.datetime.LocalDate

class GetOldestTransactionDateUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(): LocalDate? {
        return repository.getOldestTransactionDate()
    }
}
