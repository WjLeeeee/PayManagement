package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.usecase.card.ManageBalanceCardUseCase
import com.woojin.paymanagement.domain.usecase.card.ManageGiftCardUseCase

/**
 * 거래 처리와 함께 카드 관리를 통합 처리하는 UseCase
 * SRP: 거래-카드 통합 프로세스만 담당
 * OCP: 새로운 카드 타입 추가 시 확장 가능
 */
class ProcessTransactionWithCardsUseCase(
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val manageBalanceCardUseCase: ManageBalanceCardUseCase,
    private val manageGiftCardUseCase: ManageGiftCardUseCase
) {

    /**
     * 여러 거래를 처리하면서 관련 카드들도 함께 관리
     * 기존 App.kt의 복잡한 카드 관리 로직을 캡슐화
     */
    suspend fun processMultipleTransactions(transactions: List<Transaction>) {
        for (transaction in transactions) {
            // 1. 거래 저장
            saveTransactionUseCase.invoke(transaction)

            // 2. 카드 관련 처리
            processCardManagement(transaction)
        }
    }

    /**
     * 단일 거래에 대한 카드 관리 처리
     */
    private suspend fun processCardManagement(transaction: Transaction) {
        // 잔액권 관련 처리
        processBalanceCardTransaction(transaction)

        // 상품권 관련 처리
        processGiftCardTransaction(transaction)
    }

    /**
     * 잔액권 관련 거래 처리
     */
    private suspend fun processBalanceCardTransaction(transaction: Transaction) {
        when (transaction.type) {
            com.woojin.paymanagement.data.TransactionType.INCOME -> {
                if (transaction.incomeType == com.woojin.paymanagement.data.IncomeType.BALANCE_CARD) {
                    manageBalanceCardUseCase.createBalanceCardFromTransaction(transaction)
                }
            }
            com.woojin.paymanagement.data.TransactionType.EXPENSE -> {
                if (transaction.paymentMethod == com.woojin.paymanagement.data.PaymentMethod.BALANCE_CARD) {
                    manageBalanceCardUseCase.updateBalanceCardFromExpense(transaction)
                }
            }
        }
    }

    /**
     * 상품권 관련 거래 처리
     */
    private suspend fun processGiftCardTransaction(transaction: Transaction) {
        when (transaction.type) {
            com.woojin.paymanagement.data.TransactionType.INCOME -> {
                if (transaction.incomeType == com.woojin.paymanagement.data.IncomeType.GIFT_CARD) {
                    manageGiftCardUseCase.createGiftCardFromTransaction(transaction)
                }
            }
            com.woojin.paymanagement.data.TransactionType.EXPENSE -> {
                if (transaction.paymentMethod == com.woojin.paymanagement.data.PaymentMethod.GIFT_CARD) {
                    manageGiftCardUseCase.updateGiftCardFromExpense(transaction)
                }
            }
        }
    }
}