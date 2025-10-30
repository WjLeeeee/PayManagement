package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.domain.repository.CategoryRepository
import com.woojin.paymanagement.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first

class UpdateCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {
    /**
     * 카테고리를 업데이트합니다.
     * @param oldCategory 기존 카테고리 정보
     * @param newCategory 새로운 카테고리 정보
     * @throws IllegalArgumentException 이름이 중복되는 경우
     */
    suspend operator fun invoke(oldCategory: Category, newCategory: Category) {
        // 이름이 변경되었는지 확인
        val nameChanged = oldCategory.name != newCategory.name

        if (nameChanged) {
            // 같은 타입의 다른 카테고리 중에 동일한 이름이 있는지 확인
            val existingCategories = categoryRepository.getCategoriesByType(newCategory.type).first()
            val hasDuplicateName = existingCategories.any {
                it.id != newCategory.id && it.name == newCategory.name
            }

            if (hasDuplicateName) {
                throw IllegalArgumentException("이미 동일한 이름의 카테고리가 존재합니다: ${newCategory.name}")
            }
        }

        // 카테고리 업데이트
        categoryRepository.updateCategory(newCategory)

        // 이름이 변경되었다면 해당 카테고리를 사용하는 모든 거래 내역도 업데이트
        if (nameChanged) {
            transactionRepository.updateTransactionsCategoryName(
                oldCategoryName = oldCategory.name,
                newCategoryName = newCategory.name
            )
        }
    }
}
