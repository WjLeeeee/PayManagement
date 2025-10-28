package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun insertCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: String)
    suspend fun initializeDefaultCategories()
}
