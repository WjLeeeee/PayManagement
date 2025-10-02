package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.ParsedTransactionRepository

class MarkParsedTransactionProcessedUseCase(
    private val parsedTransactionRepository: ParsedTransactionRepository
) {
    suspend operator fun invoke(id: String) {
        parsedTransactionRepository.markAsProcessed(id)
    }
}