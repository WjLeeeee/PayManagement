package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.ParsedTransactionRepository
import kotlinx.datetime.Clock

/**
 * 기록 완료 후 보관 기간(3일)이 지난 카드 알림 내역 삭제
 */
class CleanupProcessedParsedTransactionsUseCase(
    private val parsedTransactionRepository: ParsedTransactionRepository
) {
    suspend operator fun invoke() {
        val before = Clock.System.now().toEpochMilliseconds() - RETENTION_MS
        parsedTransactionRepository.deleteProcessedBefore(before)
    }

    companion object {
        const val RETENTION_DAYS = 3
        const val RETENTION_MS = RETENTION_DAYS * 24L * 60 * 60 * 1000
    }
}
