package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAvailableBalanceCardsUseCaseTest {

    private val date = LocalDate(2025, 7, 1)

    // 예전에 충전해서 다 쓴 "스타벅스" 잔액권 거래
    private val usedUpOldCard = listOf(
        Transaction(
            id = "charge", amount = 10_000.0, type = TransactionType.INCOME, category = "기타", memo = "",
            date = date, incomeType = IncomeType.BALANCE_CARD, balanceCardId = "old", cardName = "스타벅스"
        ),
        Transaction(
            id = "use", amount = 10_000.0, type = TransactionType.EXPENSE, category = "카페", memo = "",
            date = date, paymentMethod = PaymentMethod.BALANCE_CARD, balanceCardId = "old", cardName = "스타벅스"
        )
    )

    @Test
    fun newDbCardWithSameNameAsUsedUpCardIsShown() {
        val newCard = BalanceCard("new", "스타벅스", 30_000.0, 30_000.0, date)

        val result = GetAvailableBalanceCardsUseCase()(usedUpOldCard, setOf("new"), listOf(newCard))

        assertEquals(listOf(newCard), result)
    }

    @Test
    fun oldCardWithBalanceKeepsPriorityOverSameNameDbCard() {
        val chargeOnly = usedUpOldCard.take(1)
        val newCard = BalanceCard("new", "스타벅스", 30_000.0, 30_000.0, date)

        val result = GetAvailableBalanceCardsUseCase()(chargeOnly, setOf("old", "new"), listOf(newCard))

        assertEquals(listOf("old"), result.map { it.id })
    }
}
