package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.ParsedTransaction
import com.woojin.paymanagement.domain.repository.ParsedTransactionRepository
import kotlinx.coroutines.flow.Flow

/**
 * 가계부에 기록 완료된 카드 알림 내역 (완료 탭)
 */
class GetProcessedParsedTransactionsUseCase(
    private val parsedTransactionRepository: ParsedTransactionRepository
) {
    operator fun invoke(): Flow<List<ParsedTransaction>> {
        return parsedTransactionRepository.getProcessedParsedTransactions()
    }
}
