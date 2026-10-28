package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAvailableGiftCardsUseCaseTest {

    private val date = LocalDate(2025, 7, 1)

    // 예전에 받은 "신세계" 상품권 거래
    private val charge = Transaction(
        id = "charge", amount = 50_000.0, type = TransactionType.INCOME, category = "기타", memo = "",
        date = date, incomeType = IncomeType.GIFT_CARD, giftCardId = "old", cardName = "신세계"
    )
    private val newCard = GiftCard("new", "신세계", 100_000.0, 0.0, date)

    @Test
    fun newDbCardWithSameNameAsUsedUpCardIsShown() {
        val use = Transaction(
            id = "use", amount = 50_000.0, type = TransactionType.EXPENSE, category = "쇼핑", memo = "",
            date = date, paymentMethod = PaymentMethod.GIFT_CARD, giftCardId = "old", cardName = "신세계"
        )

        val result = GetAvailableGiftCardsUseCase()(listOf(charge, use), setOf("new"), listOf(newCard))

        assertEquals(listOf(newCard), result)
    }

    @Test
    fun newDbCardWithSameNameAsInactiveCardIsShown() {
        // 옛 카드는 잔액이 남았지만 비활성 (80% 사용 후 환급 처리 등)
        val result = GetAvailableGiftCardsUseCase()(listOf(charge), setOf("new"), listOf(newCard))

        assertEquals(listOf(newCard), result)
    }
}
