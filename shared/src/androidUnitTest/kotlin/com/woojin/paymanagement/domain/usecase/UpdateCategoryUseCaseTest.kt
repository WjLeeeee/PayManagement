package com.woojin.paymanagement.domain.usecase

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.repository.CategoryRepositoryImpl
import com.woojin.paymanagement.data.repository.TransactionRepositoryImpl
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.database.PayManagementDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 카테고리 이름을 바꾸면 그 카테고리의 거래만 바뀌어야 함
 */
class UpdateCategoryUseCaseTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PayManagementDatabase.Schema.create(it) }
    private val db = DatabaseHelper(PayManagementDatabase(driver))
    private val updateCategory = UpdateCategoryUseCase(CategoryRepositoryImpl(db), TransactionRepositoryImpl(db))

    private fun tx(id: String, type: TransactionType, category: String, subCategory: String? = null) = Transaction(
        id = id, amount = 1_000.0, type = type, category = category, memo = "",
        date = LocalDate(2025, 7, 1), subCategory = subCategory
    )

    private suspend fun byId() = db.getAllTransactions().first().associateBy { it.id }

    @Test
    fun renamingIncomeCategoryLeavesSameNamedExpenseCategoryAlone() = runTest {
        val incomeFood = Category("income", "식비", "🍔", TransactionType.INCOME)
        db.insertCategory(incomeFood)
        db.insertCategory(Category("expense", "식비", "🍔", TransactionType.EXPENSE))
        db.insertTransaction(tx("in", TransactionType.INCOME, "식비"))
        db.insertTransaction(tx("out", TransactionType.EXPENSE, "식비"))

        updateCategory(incomeFood, incomeFood.copy(name = "식대"))

        assertEquals("식대", byId().getValue("in").category)
        assertEquals("식비", byId().getValue("out").category)
    }

    @Test
    fun renamingSubCategoryLeavesSameNamedSubCategoryUnderOtherParentAlone() = runTest {
        db.insertCategory(Category("food", "식비", "🍔", TransactionType.EXPENSE))
        db.insertCategory(Category("shop", "쇼핑", "🛍", TransactionType.EXPENSE))
        val foodEtc = Category("foodEtc", "기타", "•", TransactionType.EXPENSE, parentId = "food")
        db.insertCategory(foodEtc)
        db.insertCategory(Category("shopEtc", "기타", "•", TransactionType.EXPENSE, parentId = "shop"))
        db.insertTransaction(tx("food1", TransactionType.EXPENSE, "식비", subCategory = "기타"))
        db.insertTransaction(tx("shop1", TransactionType.EXPENSE, "쇼핑", subCategory = "기타"))

        updateCategory(foodEtc, foodEtc.copy(name = "간식"))

        assertEquals("간식", byId().getValue("food1").subCategory)
        assertEquals("기타", byId().getValue("shop1").subCategory)
    }
}
