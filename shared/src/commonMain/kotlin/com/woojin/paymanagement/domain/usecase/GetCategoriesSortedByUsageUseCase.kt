package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetCategoriesSortedByUsageUseCase(
    private val categoryRepository: CategoryRepository,
    private val databaseHelper: DatabaseHelper
) {
    operator fun invoke(type: TransactionType): Flow<List<Category>> {
        return combine(
            categoryRepository.getCategoriesByType(type),
            databaseHelper.getAllTransactions()
        ) { categories, transactions ->
            val usageCount = transactions
                .filter { it.type == type }
                .groupBy { it.category }
                .mapValues { it.value.size }

            // 사용 횟수 많은 순으로 정렬, 한 번도 안 쓴 카테고리는 기존 순서 유지
            val used = categories.filter { (usageCount[it.name] ?: 0) > 0 }
                .sortedByDescending { usageCount[it.name] ?: 0 }
            val unused = categories.filter { (usageCount[it.name] ?: 0) == 0 }

            used + unused
        }
    }
}
