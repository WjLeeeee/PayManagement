package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.ParsedTransactionRepository

class DeleteParsedTransactionUseCase(
    private val parsedTransactionRepository: ParsedTransactionRepository
) {
    suspend operator fun invoke(id: String) {
        parsedTransactionRepository.deleteParsedTransaction(id)
    }
}