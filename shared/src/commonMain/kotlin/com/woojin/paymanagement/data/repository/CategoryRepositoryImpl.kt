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
            Category(uuid4().toString(), "당근", "🥕", TransactionType.INCOME, sortOrder = 2),
            Category(uuid4().toString(), "K-패스 환급", "🚌", TransactionType.INCOME, sortOrder = 3),
            Category(uuid4().toString(), "익절", "📈", TransactionType.INCOME, sortOrder = 4),
            Category(uuid4().toString(), "기타수입", "💵", TransactionType.INCOME, sortOrder = 5)
        )

        // 지출 카테고리
        val expenseCategories = listOf(
            Category(uuid4().toString(), "식비", "🍔", TransactionType.EXPENSE, sortOrder = 0),
            Category(uuid4().toString(), "데이트", "💑", TransactionType.EXPENSE, sortOrder = 1),
            Category(uuid4().toString(), "생활비", "🏠", TransactionType.EXPENSE, sortOrder = 2),
            Category(uuid4().toString(), "생활용품", "🧴", TransactionType.EXPENSE, sortOrder = 3),
            Category(uuid4().toString(), "쇼핑", "🛍️", TransactionType.EXPENSE, sortOrder = 4),
            Category(uuid4().toString(), "문화생활", "🎬", TransactionType.EXPENSE, sortOrder = 5),
            Category(uuid4().toString(), "경조사", "🎁", TransactionType.EXPENSE, sortOrder = 6),
            Category(uuid4().toString(), "자기계발", "📚", TransactionType.EXPENSE, sortOrder = 7),
            Category(uuid4().toString(), "공과금", "💡", TransactionType.EXPENSE, sortOrder = 8),
            Category(uuid4().toString(), "대출이자", "🏦", TransactionType.EXPENSE, sortOrder = 9),
            Category(uuid4().toString(), "모임통장", "👥", TransactionType.EXPENSE, sortOrder = 10),
            Category(uuid4().toString(), "교통비", "🚗", TransactionType.EXPENSE, sortOrder = 11),
            Category(uuid4().toString(), "적금", "🐷", TransactionType.EXPENSE, sortOrder = 12),
            Category(uuid4().toString(), "투자", "💹", TransactionType.EXPENSE, sortOrder = 13),
            Category(uuid4().toString(), "손절", "📉", TransactionType.EXPENSE, sortOrder = 14),
            Category(uuid4().toString(), "정기결제", "📅", TransactionType.EXPENSE, sortOrder = 15),
            Category(uuid4().toString(), "기타지출", "💸", TransactionType.EXPENSE, sortOrder = 16)
        )

        // 모든 카테고리 삽입
        (incomeCategories + expenseCategories).forEach { category ->
            databaseHelper.insertCategory(category)
        }
    }
}
