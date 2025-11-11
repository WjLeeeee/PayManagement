package com.woojin.paymanagement.presentation.budgetsettings

import androidx.compose.ui.text.input.TextFieldValue
import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.CategoryBudget
import com.woojin.paymanagement.utils.PayPeriod

data class BudgetSettingsUiState(
    val currentPeriod: PayPeriod? = null,
    val selectedTab: BudgetTab = BudgetTab.SETTINGS,
    val monthlySalary: String = "",
    val categoryBudgets: List<CategoryBudgetWithProgress> = emptyList(),
    val totalAllocated: Double = 0.0,
    val unallocated: Double = 0.0,
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showAddCategoryDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingBudget: CategoryBudgetWithProgress? = null,
    val editAmount: TextFieldValue = TextFieldValue(""),
    val editMemo: String = "",  // 예산 수정 시 메모
    val availableCategories: List<Category> = emptyList(),
    val selectedCategories: Set<Category> = emptySet(),
    val groupName: String = "",
    val newBudgetAmount: TextFieldValue = TextFieldValue(""),
    val newBudgetMemo: String = ""  // 예산 추가 시 메모
)

enum class BudgetTab {
    SETTINGS,   // 예산 설정
    PROGRESS    // 사용 현황
}

data class CategoryBudgetWithProgress(
    val categoryBudget: CategoryBudget,
    val spentAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val progress: Float = 0f,
    val categories: List<Category> = emptyList(),  // 그룹인 경우 하위 카테고리 정보
    val categorySpentAmounts: Map<String, Double> = emptyMap()  // 카테고리별 지출 금액 (categoryId -> amount)
) {
    val isOverBudget: Boolean
        get() = spentAmount > categoryBudget.allocatedAmount
}
