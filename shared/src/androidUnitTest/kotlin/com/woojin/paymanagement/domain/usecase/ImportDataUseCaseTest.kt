package com.woojin.paymanagement.domain.usecase

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.woojin.paymanagement.data.BackupData
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionBackup
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.database.PayManagementDatabase
import com.woojin.paymanagement.domain.repository.PreferencesRepository
import com.woojin.paymanagement.utils.PaydayAdjustment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 복원이 실패하면 기존 데이터가 그대로 남아야 함 (전부 되거나 전부 안 되거나)
 */
class ImportDataUseCaseTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PayManagementDatabase.Schema.create(it) }
    private val db = DatabaseHelper(PayManagementDatabase(driver))
    private val prefs = FakePreferences()
    private val import = ImportDataUseCase(db, prefs)

    private val existing = Transaction(
        id = "existing", amount = 10_000.0, type = TransactionType.EXPENSE, category = "식비",
        memo = "", date = LocalDate(2025, 7, 1)
    )

    private fun backupTx(id: String, paymentMethod: String? = "CASH") = TransactionBackup(
        id = id, date = "2025-07-02", amount = 5_000.0, type = "EXPENSE", category = "카페", memo = "",
        paymentMethod = paymentMethod, incomeType = null, balanceCardId = null, cardName = null
    )

    private fun json(backup: BackupData) = Json.encodeToString(backup)

    @Test
    fun unreadableRowKeepsExistingData() = runTest {
        db.insertTransaction(existing)

        val result = import(json(BackupData(transactions = listOf(backupTx("new"), backupTx("bad", "UNKNOWN")))), replaceExisting = true)

        assertTrue(result.isFailure)
        assertEquals(listOf("existing"), db.getAllTransactions().first().map { it.id })
    }

    @Test
    fun unreadablePaydaySettingKeepsExistingDataAndSettings() = runTest {
        db.insertTransaction(existing)

        val result = import(
            json(BackupData(payday = 25, paydayAdjustment = "NONE", transactions = listOf(backupTx("new")))),
            replaceExisting = true
        )

        assertTrue(result.isFailure)
        assertEquals(listOf("existing"), db.getAllTransactions().first().map { it.id })
        assertNull(prefs.payday)
    }

    @Test
    fun dbErrorMidwayRollsBackDeletes() = runTest {
        db.insertTransaction(existing)

        // 같은 ID 두 번 → 두 번째 INSERT가 PK 충돌로 실패. 앞의 삭제·삽입도 되돌아가야 함
        val result = import(json(BackupData(transactions = listOf(backupTx("dup"), backupTx("dup")))), replaceExisting = true)

        assertTrue(result.isFailure)
        assertEquals(listOf("existing"), db.getAllTransactions().first().map { it.id })
    }

    @Test
    fun validBackupReplacesDataAndSettings() = runTest {
        db.insertTransaction(existing)

        val result = import(
            json(BackupData(payday = 10, paydayAdjustment = "AFTER_WEEKEND", transactions = listOf(backupTx("a"), backupTx("b")))),
            replaceExisting = true
        )

        assertEquals(2, result.getOrThrow().successCount)
        assertEquals(setOf("a", "b"), db.getAllTransactions().first().map { it.id }.toSet())
        assertEquals(10, prefs.payday)
        assertEquals(PaydayAdjustment.AFTER_WEEKEND, prefs.adjustment)
    }

    @Test
    fun oldGiftCardBackupIsImportedAsCash() = runTest {
        val giftExpense = backupTx("giftExpense", paymentMethod = "GIFT_CARD")
        val giftIncome = backupTx("giftIncome", paymentMethod = null).copy(type = "INCOME", incomeType = "GIFT_CARD")

        import(json(BackupData(transactions = listOf(giftExpense, giftIncome))), replaceExisting = true).getOrThrow()

        val byId = db.getAllTransactions().first().associateBy { it.id }
        assertEquals(PaymentMethod.CASH, byId.getValue("giftExpense").paymentMethod)
        assertEquals(IncomeType.CASH, byId.getValue("giftIncome").incomeType)
    }

    @Test
    fun oldGiftCardRowInDbIsReadAsCash() = runTest {
        // 상품권 기능 제거 전 앱이 저장한 행
        driver.execute(
            null,
            "INSERT INTO TransactionEntity(id, amount, type, category, memo, date, paymentMethod, isSettlement) " +
                "VALUES ('old', 5000.0, 'EXPENSE', '식비', '', '2025-07-01', 'GIFT_CARD', 0)",
            0
        )

        assertEquals(PaymentMethod.CASH, db.getAllTransactions().first().single().paymentMethod)
    }

    private class FakePreferences : PreferencesRepository {
        var payday: Int? = null
        var adjustment: PaydayAdjustment? = null
        override fun setPayday(day: Int) { payday = day }
        override fun setPaydayAdjustment(adjustment: PaydayAdjustment) { this.adjustment = adjustment }
        override fun getPayday() = TODO()
        override fun getPaydayAdjustment() = TODO()
        override fun isMoneyVisible() = TODO()
        override fun setMoneyVisible(visible: Boolean) = TODO()
        override fun getMonthlySalary() = TODO()
        override fun setMonthlySalary(salary: Double) = TODO()
        override fun isSharedMode() = TODO()
        override fun setIsSharedMode(enabled: Boolean) = TODO()
        override fun getHolidaysRefreshedAt() = TODO()
        override fun setHolidaysRefreshedAt(time: Long) = TODO()
    }
}
