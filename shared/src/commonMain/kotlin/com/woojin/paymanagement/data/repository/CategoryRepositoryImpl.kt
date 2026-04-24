package com.woojin.paymanagement.data.repository

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.benasher44.uuid.uuid4

class CategoryRepositoryImpl(
    private val databaseHelper: DatabaseHelper
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return databaseHelper.getAllCategories()
    }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return databaseHelper.getCategoriesByType(type)
    }

    override suspend fun getCategoryById(id: String): Category? {
        return databaseHelper.getCategoryById(id)
    }

    override suspend fun insertCategory(category: Category) {
        databaseHelper.insertCategory(category)
    }

    override suspend fun updateCategory(category: Category) {
        databaseHelper.updateCategory(category)
    }

    override suspend fun deleteCategory(id: String) {
        databaseHelper.deleteCategory(id)
    }

    override suspend fun initializeDefaultCategories() {
        // 이미 카테고리가 있으면 초기화하지 않음
        val existingCategories = databaseHelper.getAllCategories().first()
        if (existingCategories.isNotEmpty()) return

        // 수입 카테고리
        val incomeCategories = listOf(
            Category(uuid4().toString(), "급여", "💰", TransactionType.INCOME, sortOrder = 0),
            Category(uuid4().toString(), "식비", "🍔", TransactionType.INCOME, sortOrder = 1),
            Category(uuid4().toString(), "기타수입", "💵", TransactionType.INCOME, sortOrder = 2)
        )

        // 지출 카테고리
        val expenseCategories = listOf(
            Category(uuid4().toString(), "식비", "🍔", TransactionType.EXPENSE, sortOrder = 0),
            Category(uuid4().toString(), "생활비", "🏠", TransactionType.EXPENSE, sortOrder = 1),
            Category(uuid4().toString(), "생활용품", "🧴", TransactionType.EXPENSE, sortOrder = 2),
            Category(uuid4().toString(), "쇼핑", "🛍️", TransactionType.EXPENSE, sortOrder = 3),
            Category(uuid4().toString(), "문화생활", "🎬", TransactionType.EXPENSE, sortOrder = 4),
            Category(uuid4().toString(), "교통비", "🚗", TransactionType.EXPENSE, sortOrder = 5),
            Category(uuid4().toString(), "기타지출", "💸", TransactionType.EXPENSE, sortOrder = 6)
        )

        // 저축 카테고리
        val savingCategories = listOf(
            Category(uuid4().toString(), "적금", "🐷", TransactionType.SAVING, sortOrder = 0),
            Category(uuid4().toString(), "예금", "🏦", TransactionType.SAVING, sortOrder = 1)
        )

        // 투자 카테고리
        val investmentCategories = listOf(
            Category(uuid4().toString(), "투자", "💹", TransactionType.INVESTMENT, sortOrder = 0),
            Category(uuid4().toString(), "익절", "📈", TransactionType.INVESTMENT, sortOrder = 1),
            Category(uuid4().toString(), "손절", "📉", TransactionType.INVESTMENT, sortOrder = 2),
            Category(uuid4().toString(), "배당금", "💰", TransactionType.INVESTMENT, sortOrder = 3)
        )

        // 모든 카테고리 삽입
        (incomeCategories + expenseCategories + savingCategories + investmentCategories).forEach { category ->
            databaseHelper.insertCategory(category)
        }
    }
}
