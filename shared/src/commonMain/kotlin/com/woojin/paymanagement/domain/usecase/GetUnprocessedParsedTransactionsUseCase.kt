package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.ParsedTransaction
import com.woojin.paymanagement.domain.repository.ParsedTransactionRepository
import kotlinx.coroutines.flow.Flow

class GetUnprocessedParsedTransactionsUseCase(
    private val parsedTransactionRepository: ParsedTransactionRepository
) {
    operator fun invoke(): Flow<List<ParsedTransaction>> {
        return parsedTransactionRepository.getUnprocessedParsedTransactions()
    }
}