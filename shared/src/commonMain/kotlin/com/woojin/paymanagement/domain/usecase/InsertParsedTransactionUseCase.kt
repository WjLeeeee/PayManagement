package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.ParsedTransaction
import com.woojin.paymanagement.domain.repository.ParsedTransactionRepository

class InsertParsedTransactionUseCase(
    private val parsedTransactionRepository: ParsedTransactionRepository
) {
    suspend operator fun invoke(parsedTransaction: ParsedTransaction): Boolean {
        return parsedTransactionRepository.insertParsedTransaction(parsedTransaction)
    }
}