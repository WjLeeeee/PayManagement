package com.woojin.paymanagement.presentation.budgetsettings

import androidx.compose.ui.text.input.TextFieldValue
import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.CategoryBudget
import com.woojin.paymanagement.data.TransferItem
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.removeCommas

data class BudgetSettingsUiState(
    val currentPeriod: PayPeriod? = null,  // 실제 현재 급여 기간
    val viewingPeriod: PayPeriod? = null,  // 사용 현황에서 보고 있는 급여 기간
    val canNavigateNext: Boolean = false,  // 다음 기간으로 이동 가능한지 (미래 기간 체크)
    val canNavigatePrevious: Boolean = true,  // 이전 기간으로 이동 가능한지 (거래 내역 체크)
    val selectedTab: BudgetTab = BudgetTab.SETTINGS,
    val budgetPlanId: String? = null,  // 현재 로드된 예산 템플릿 id
    val transfers: List<TransferItem> = emptyList(),  // 월급날 이체 계획
    val showTransferPlanSheet: Boolean = false,
    val isTransferPlanEditing: Boolean = false,  // 바텀시트 내 편집 모드
    val transferDrafts: List<BudgetItemDraft> = emptyList(),  // 이체 계획 편집 중 (name = 통장 이름)
    val transferChecks: Set<String> = emptySet(),  // viewingPeriod에 이체 완료 체크된 TransferItem id
    val monthlySalary: TextFieldValue = TextFieldValue(""),
    val isSalaryEditing: Boolean = false,  // 급여 편집 모드 여부
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
    val editItems: List<BudgetItemDraft> = emptyList(),  // 예산 수정 시 세부 항목
    val editAccounts: List<AccountDraft> = emptyList(),  // 예산 수정 시 사용 통장과 통장별 금액
    val editAvailableCategories: List<Category> = emptyList(),  // 수정 다이얼로그용 선택 가능한 카테고리
    val editSelectedCategories: Set<Category> = emptySet(),  // 수정 다이얼로그에서 선택된 카테고리
    val editGroupName: String = "",  // 수정 다이얼로그에서 그룹명
    val availableCategories: List<Category> = emptyList(),
    val selectedCategories: Set<Category> = emptySet(),
    val groupName: String = "",
    val newBudgetAmount: TextFieldValue = TextFieldValue(""),
    val newBudgetMemo: String = "",  // 예산 추가 시 메모
    val newBudgetItems: List<BudgetItemDraft> = emptyList(),  // 예산 추가 시 세부 항목
    val newBudgetAccounts: List<AccountDraft> = emptyList()  // 예산 추가 시 사용 통장과 통장별 금액
) {
    /** 카드 표시용: 이체 계획 순서대로 (통장 이름, 배분 금액). 통장이 하나면 금액은 null */
    fun accountSummaryOf(budget: CategoryBudget): List<Pair<String, Double?>> {
        val single = budget.accountAllocations.size == 1
        return transfers
            .filter { it.id in budget.transferItemIds }
            .map { it.accountName to if (single) null else budget.amountForAccount(it.id) }
    }
}

/**
 * 다이얼로그에서 편집 중인 사용 통장 (금액은 통장이 2개 이상일 때만 의미 있음)
 */
data class AccountDraft(
    val transferItemId: String,
    val amount: TextFieldValue = TextFieldValue("")
) {
    val amountValue: Double
        get() = removeCommas(amount.text).toDoubleOrNull() ?: 0.0
}

/**
 * 다이얼로그에서 편집 중인 세부 항목 (금액은 쉼표 포맷된 입력값)
 */
data class BudgetItemDraft(
    val id: String,
    val name: String = "",
    val amount: TextFieldValue = TextFieldValue("")
) {
    val amountValue: Double
        get() = removeCommas(amount.text).toDoubleOrNull() ?: 0.0
}

val List<BudgetItemDraft>.totalAmount: Double
    get() = sumOf { it.amountValue }

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
