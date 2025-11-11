package com.woojin.paymanagement.presentation.budgetsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.woojin.paymanagement.data.BudgetPlan
import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.CategoryBudget
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.repository.PreferencesRepository
import com.woojin.paymanagement.domain.usecase.DeleteCategoryBudgetUseCase
import com.woojin.paymanagement.domain.usecase.GetCategoriesUseCase
import com.woojin.paymanagement.domain.usecase.GetCategoryBudgetsUseCase
import com.woojin.paymanagement.domain.usecase.GetCurrentBudgetPlanUseCase
import com.woojin.paymanagement.domain.usecase.GetSpentAmountByCategoryUseCase
import com.woojin.paymanagement.domain.usecase.SaveBudgetPlanUseCase
import com.woojin.paymanagement.domain.usecase.SaveCategoryBudgetUseCase
import com.woojin.paymanagement.domain.usecase.UpdateCategoryBudgetUseCase
import com.woojin.paymanagement.utils.PayPeriodCalculator
import com.woojin.paymanagement.utils.formatWithCommas
import com.woojin.paymanagement.utils.removeCommas
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class BudgetSettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val getCurrentBudgetPlanUseCase: GetCurrentBudgetPlanUseCase,
    private val saveBudgetPlanUseCase: SaveBudgetPlanUseCase,
    private val getCategoryBudgetsUseCase: GetCategoryBudgetsUseCase,
    private val saveCategoryBudgetUseCase: SaveCategoryBudgetUseCase,
    private val updateCategoryBudgetUseCase: UpdateCategoryBudgetUseCase,
    private val deleteCategoryBudgetUseCase: DeleteCategoryBudgetUseCase,
    private val getSpentAmountByCategoryUseCase: GetSpentAmountByCategoryUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    var uiState by mutableStateOf(BudgetSettingsUiState())
        private set

    private var budgetJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)

            try {
                // 급여 정보 로드
                val payday = preferencesRepository.getPayday()
                val adjustment = preferencesRepository.getPaydayAdjustment()
                val monthlySalary = preferencesRepository.getMonthlySalary()

                // 현재 급여 사이클 계산
                val currentPeriod = PayPeriodCalculator.getCurrentPayPeriod(
                    payday = payday,
                    adjustment = adjustment
                )

                uiState = uiState.copy(
                    currentPeriod = currentPeriod,
                    monthlySalary = if (monthlySalary > 0) formatWithCommas(monthlySalary.toLong()) else ""
                )

                // 예산 계획 및 카테고리 예산 로드
                loadBudgetData(currentPeriod)

            } catch (e: Exception) {
                uiState = uiState.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    private fun loadBudgetData(payPeriod: com.woojin.paymanagement.utils.PayPeriod) {
        budgetJob?.cancel()

        budgetJob = viewModelScope.launch {
            try {
                getCurrentBudgetPlanUseCase(payPeriod).collect { budgetPlan ->
                    if (budgetPlan != null) {
                        // 예산 계획이 있으면 카테고리 예산 로드
                        loadCategoryBudgets(budgetPlan.id, payPeriod)
                    } else {
                        // 예산 계획이 없으면 빈 상태
                        uiState = uiState.copy(
                            categoryBudgets = emptyList(),
                            totalAllocated = 0.0,
                            unallocated = getSalaryAmount(),
                            totalSpent = 0.0,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun loadCategoryBudgets(
        budgetPlanId: String,
        payPeriod: com.woojin.paymanagement.utils.PayPeriod
    ) {
        getCategoryBudgetsUseCase(budgetPlanId).collect { categoryBudgets ->
            // 모든 지출 카테고리 정보 가져오기
            val allCategories = getCategoriesUseCase(TransactionType.EXPENSE).first()

            // 각 카테고리별 사용 금액 계산
            val budgetsWithProgress = categoryBudgets.map { budget ->
                // 그룹인 경우 하위 카테고리 정보 수집
                val categories = if (budget.isGroup) {
                    budget.categoryIds.mapNotNull { categoryId ->
                        allCategories.find { it.id == categoryId }
                    }
                } else {
                    emptyList()
                }

                // 카테고리별 지출 금액 계산
                val categorySpentAmounts = mutableMapOf<String, Double>()
                val spent = if (budget.isGroup) {
                    // 그룹: 모든 categoryIds의 지출을 합산
                    budget.categoryIds.sumOf { categoryId ->
                        val category = allCategories.find { it.id == categoryId }
                        if (category != null) {
                            val categorySpent = getSpentAmountByCategoryUseCase(
                                categoryName = category.name,
                                startDate = payPeriod.startDate,
                                endDate = payPeriod.endDate
                            )
                            categorySpentAmounts[categoryId] = categorySpent
                            categorySpent
                        } else {
                            0.0
                        }
                    }
                } else {
                    // 단일 카테고리: 기존 로직 사용
                    getSpentAmountByCategoryUseCase(
                        categoryName = budget.categoryName,
                        startDate = payPeriod.startDate,
                        endDate = payPeriod.endDate
                    )
                }

                val remaining = budget.allocatedAmount - spent
                val progress = if (budget.allocatedAmount > 0) {
                    (spent / budget.allocatedAmount).toFloat().coerceAtLeast(0f)
                } else {
                    0f
                }

                CategoryBudgetWithProgress(
                    categoryBudget = budget,
                    spentAmount = spent,
                    remainingAmount = remaining,
                    progress = progress,
                    categories = categories,
                    categorySpentAmounts = categorySpentAmounts
                )
            }

            val totalAllocated = categoryBudgets.sumOf { it.allocatedAmount }
            val totalSpent = budgetsWithProgress.sumOf { it.spentAmount }
            val salary = getSalaryAmount()

            uiState = uiState.copy(
                categoryBudgets = budgetsWithProgress,
                totalAllocated = totalAllocated,
                unallocated = salary - totalAllocated,
                totalSpent = totalSpent,
                isLoading = false
            )
        }
    }

    fun selectTab(tab: BudgetTab) {
        uiState = uiState.copy(selectedTab = tab)
    }

    fun updateMonthlySalary(salary: String) {
        // 쉼표 제거 후 숫자만 추출
        val digitsOnly = removeCommas(salary)

        if (digitsOnly.isEmpty() || digitsOnly.matches(Regex("^\\d+$"))) {
            val formattedSalary = if (digitsOnly.isNotEmpty()) {
                val number = digitsOnly.toLongOrNull() ?: 0L
                formatWithCommas(number)
            } else {
                ""
            }

            uiState = uiState.copy(monthlySalary = formattedSalary)

            // PreferencesRepository에 저장
            val salaryAmount = digitsOnly.toDoubleOrNull() ?: 0.0
            preferencesRepository.setMonthlySalary(salaryAmount)

            // 미배분 금액 재계산
            uiState = uiState.copy(
                unallocated = salaryAmount - uiState.totalAllocated
            )
        }
    }

    fun showAddCategoryDialog() {
        viewModelScope.launch {
            // 현재 예산이 설정되지 않은 지출 카테고리 조회
            val allCategories = getCategoriesUseCase(TransactionType.EXPENSE).first()
            val usedCategoryIds = uiState.categoryBudgets.flatMap { it.categoryBudget.categoryIds }.toSet()
            val availableCategories = allCategories.filter { it.id !in usedCategoryIds }

            uiState = uiState.copy(
                showAddCategoryDialog = true,
                availableCategories = availableCategories,
                selectedCategories = emptySet(),
                groupName = "",
                newBudgetAmount = TextFieldValue("")
            )
        }
    }

    fun hideAddCategoryDialog() {
        uiState = uiState.copy(
            showAddCategoryDialog = false,
            selectedCategories = emptySet(),
            groupName = "",
            newBudgetAmount = TextFieldValue(""),
            newBudgetMemo = ""
        )
    }

    fun toggleCategorySelection(category: Category) {
        val currentSelection = uiState.selectedCategories
        val newSelection = if (category in currentSelection) {
            currentSelection - category
        } else {
            currentSelection + category
        }
        uiState = uiState.copy(selectedCategories = newSelection)
    }

    fun updateGroupName(name: String) {
        uiState = uiState.copy(groupName = name)
    }

    fun updateNewBudgetAmount(newValue: TextFieldValue) {
        // 쉼표 제거 후 숫자만 추출
        val digitsOnly = removeCommas(newValue.text)

        if (digitsOnly.isEmpty() || digitsOnly.matches(Regex("^\\d+$"))) {
            val formattedAmount = if (digitsOnly.isNotEmpty()) {
                val number = digitsOnly.toLongOrNull() ?: 0L
                formatWithCommas(number)
            } else {
                ""
            }

            // 커서를 항상 텍스트 끝에 위치시켜 자연스러운 숫자 입력 제공
            uiState = uiState.copy(
                newBudgetAmount = TextFieldValue(
                    text = formattedAmount,
                    selection = TextRange(formattedAmount.length)
                )
            )
        }
    }

    fun addCategoryBudget() {
        val selectedCategories = uiState.selectedCategories
        if (selectedCategories.isEmpty()) return

        val amount = removeCommas(uiState.newBudgetAmount.text).toDoubleOrNull() ?: return
        val period = uiState.currentPeriod ?: return

        if (amount <= 0) return

        viewModelScope.launch {
            try {
                uiState = uiState.copy(isSaving = true)

                // 예산 계획이 없으면 생성
                var budgetPlan = getCurrentBudgetPlanUseCase(period).first()
                if (budgetPlan == null) {
                    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    budgetPlan = BudgetPlan(
                        id = uuid4().toString(),
                        periodStartDate = period.startDate,
                        periodEndDate = period.endDate,
                        createdAt = today
                    )
                    saveBudgetPlanUseCase(budgetPlan)
                }

                // 카테고리 예산 추가
                val memo = uiState.newBudgetMemo.ifBlank { null }
                val categoryBudget = if (selectedCategories.size == 1) {
                    // 단일 카테고리
                    val category = selectedCategories.first()
                    CategoryBudget(
                        id = uuid4().toString(),
                        budgetPlanId = budgetPlan.id,
                        categoryIds = listOf(category.id),
                        categoryName = category.name,
                        categoryEmoji = category.emoji,
                        allocatedAmount = amount,
                        memo = memo
                    )
                } else {
                    // 카테고리 그룹
                    val groupName = uiState.groupName.ifBlank {
                        selectedCategories.joinToString(", ") { it.name }
                    }

                    CategoryBudget(
                        id = uuid4().toString(),
                        budgetPlanId = budgetPlan.id,
                        categoryIds = selectedCategories.map { it.id },
                        categoryName = groupName,
                        categoryEmoji = "📦",  // 그룹은 항상 📦 이모지 사용
                        allocatedAmount = amount,
                        memo = memo
                    )
                }

                saveCategoryBudgetUseCase(categoryBudget)

                hideAddCategoryDialog()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            } finally {
                uiState = uiState.copy(isSaving = false)
            }
        }
    }

    fun showEditDialog(budget: CategoryBudgetWithProgress) {
        val formattedAmount = formatWithCommas(budget.categoryBudget.allocatedAmount.toLong())
        uiState = uiState.copy(
            showEditDialog = true,
            editingBudget = budget,
            editAmount = TextFieldValue(
                text = formattedAmount,
                selection = TextRange(formattedAmount.length)
            ),
            editMemo = budget.categoryBudget.memo ?: ""
        )
    }

    fun hideEditDialog() {
        uiState = uiState.copy(
            showEditDialog = false,
            editingBudget = null,
            editAmount = TextFieldValue(""),
            editMemo = ""
        )
    }

    fun updateEditAmount(newValue: TextFieldValue) {
        // 쉼표 제거 후 숫자만 추출
        val digitsOnly = removeCommas(newValue.text)

        if (digitsOnly.isEmpty() || digitsOnly.matches(Regex("^\\d+$"))) {
            val formattedAmount = if (digitsOnly.isNotEmpty()) {
                val number = digitsOnly.toLongOrNull() ?: 0L
                formatWithCommas(number)
            } else {
                ""
            }

            // 커서를 항상 텍스트 끝에 위치시켜 자연스러운 숫자 입력 제공
            uiState = uiState.copy(
                editAmount = TextFieldValue(
                    text = formattedAmount,
                    selection = TextRange(formattedAmount.length)
                )
            )
        }
    }

    fun updateCategoryBudget() {
        val editing = uiState.editingBudget ?: return
        val amount = removeCommas(uiState.editAmount.text).toDoubleOrNull() ?: return

        if (amount <= 0) return

        viewModelScope.launch {
            try {
                uiState = uiState.copy(isSaving = true)
                val memo = uiState.editMemo.ifBlank { null }
                updateCategoryBudgetUseCase(editing.categoryBudget.id, amount, memo)
                hideEditDialog()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            } finally {
                uiState = uiState.copy(isSaving = false)
            }
        }
    }

    fun deleteCategoryBudget(budget: CategoryBudgetWithProgress) {
        viewModelScope.launch {
            try {
                deleteCategoryBudgetUseCase(budget.categoryBudget.id)
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }

    fun updateEditMemo(newMemo: String) {
        uiState = uiState.copy(editMemo = newMemo)
    }

    fun updateNewBudgetMemo(newMemo: String) {
        uiState = uiState.copy(newBudgetMemo = newMemo)
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    private fun getSalaryAmount(): Double {
        return removeCommas(uiState.monthlySalary).toDoubleOrNull() ?: 0.0
    }
}
