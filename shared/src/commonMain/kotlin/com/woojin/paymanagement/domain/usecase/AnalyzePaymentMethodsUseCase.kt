package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.PaymentMethodAnalyzer
import com.woojin.paymanagement.data.PaymentMethodSummary
import com.woojin.paymanagement.data.Transaction

class AnalyzePaymentMethodsUseCase {
    operator fun invoke(
        transactions: List<Transaction>,
        availableBalanceCards: List<BalanceCard>,
        availableGiftCards: List<GiftCard>
    ): PaymentMethodSummary {
        return PaymentMethodAnalyzer.analyzePaymentMethods(
            transactions = transactions,
            availableBalanceCards = availableBalanceCards,
            availableGiftCards = availableGiftCards
        )
    }
}