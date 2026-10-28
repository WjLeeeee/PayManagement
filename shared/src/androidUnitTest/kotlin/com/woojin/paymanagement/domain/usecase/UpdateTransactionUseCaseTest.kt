package com.woojin.paymanagement.domain.usecase

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.repository.TransactionRepositoryImpl
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.database.PayManagementDatabase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 잔액권 충전(수입) 거래를 고치면 카드 잔액도 같이 바뀌어야 함
 */
class UpdateTransactionUseCaseTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PayManagementDatabase.Schema.create(it) }
    private val db = DatabaseHelper(PayManagementDatabase(driver))
    private val update = UpdateTransactionUseCase(TransactionRepositoryImpl(db), db)

    private fun charge(id: String, amount: Double, date: LocalDate) = Transaction(
        id = id, amount = amount, type = TransactionType.INCOME, category = "기타수입", memo = "",
        date = date, incomeType = IncomeType.BALANCE_CARD, balanceCardId = "card", cardName = "편의점"
    )

    @Test
    fun editingCardCreationAmountUpdatesBalanceAndInitialAmount() = runTest {
        val created = LocalDate(2025, 7, 1)
        db.insertBalanceCard(BalanceCard("card", "편의점", 100_000.0, 100_000.0, created))
        val original = charge("charge", 100_000.0, created)
        db.insertTransaction(original)

        update(original.copy(amount = 10_000.0))

        val card = db.getBalanceCardById("card")!!
        assertEquals(10_000.0, card.currentBalance)
        assertEquals(10_000.0, card.initialAmount)
    }

    @Test
    fun editingLaterChargeOnlyShiftsBalanceByDifference() = runTest {
        // 50,000 생성 → 20,000 사용 → 20,000 충전 = 잔액 50,000
        db.insertBalanceCard(BalanceCard("card", "편의점", 50_000.0, 50_000.0, LocalDate(2025, 7, 1)))
        val laterCharge = charge("charge2", 20_000.0, LocalDate(2025, 7, 5))
        db.insertTransaction(laterCharge)

        update(laterCharge.copy(amount = 5_000.0))

        val card = db.getBalanceCardById("card")!!
        assertEquals(35_000.0, card.currentBalance)
        assertEquals(50_000.0, card.initialAmount)
    }
}
