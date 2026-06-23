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
        val nameChanged = oldCategory.name != newCategory.name
        val isSubCategory = newCategory.parentId != null

        if (nameChanged) {
            val existingCategories = categoryRepository.getCategoriesByType(newCategory.type).first()
            val hasDuplicateName = if (isSubCategory) {
                // 소분류: 같은 부모 아래에서만 중복 체크
                existingCategories.any {
                    it.id != newCategory.id && it.parentId == newCategory.parentId && it.name == newCategory.name
                }
            } else {
                // 상위 카테고리: 상위 카테고리끼리만 중복 체크
                existingCategories.any {
                    it.id != newCategory.id && it.parentId == null && it.name == newCategory.name
                }
            }

            if (hasDuplicateName) {
                throw IllegalArgumentException("이미 동일한 이름의 카테고리가 존재합니다: ${newCategory.name}")
            }
        }

        categoryRepository.updateCategory(newCategory)

        if (nameChanged) {
            if (isSubCategory) {
                transactionRepository.updateTransactionsSubCategoryName(
                    oldSubCategoryName = oldCategory.name,
                    newSubCategoryName = newCategory.name
                )
            } else {
                transactionRepository.updateTransactionsCategoryName(
                    oldCategoryName = oldCategory.name,
                    newCategoryName = newCategory.name
                )
            }
        }
    }
}
