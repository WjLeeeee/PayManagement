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
import com.woojin.paymanagement.domain.usecase.DeleteBudgetPlanUseCase
import com.woojin.paymanagement.domain.usecase.SaveCategoryBudgetUseCase
import com.woojin.paymanagement.domain.usecase.UpdateCategoryBudgetUseCase
import com.woojin.paymanagement.domain.usecase.GetOldestTransactionDateUseCase
import com.woojin.paymanagement.utils.PayPeriodCalculator
import com.woojin.paymanagement.utils.formatWithCommas
import com.woojin.paymanagement.utils.removeCommas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class BudgetSettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val getCurrentBudgetPlanUseCase: GetCurrentBudgetPlanUseCase,
    private val saveBudgetPlanUseCase: SaveBudgetPlanUseCase,
    private val deleteBudgetPlanUseCase: DeleteBudgetPlanUseCase,
    private val getCategoryBudgetsUseCase: GetCategoryBudgetsUseCase,
    private val saveCategoryBudgetUseCase: SaveCategoryBudgetUseCase,
    private val updateCategoryBudgetUseCase: UpdateCategoryBudgetUseCase,
    private val deleteCategoryBudgetUseCase: DeleteCategoryBudgetUseCase,
    private val getSpentAmountByCategoryUseCase: GetSpentAmountByCategoryUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getOldestTransactionDateUseCase: GetOldestTransactionDateUseCase,
    private val payPeriodCalculator: PayPeriodCalculator
) : ViewModel() {

    var uiState by mutableStateOf(BudgetSettingsUiState())
        private set

    private var budgetJob: Job? = null
    private var salaryUpdateJob: Job? = null  // 급여 자동 저장용

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)

            try {
                // 현재 날짜 기준으로 유효한 예산 템플릿 로드
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                // 급여일 정보 (사용 현황 탭에서 기간 계산용)
                val payday = preferencesRepository.getPayday()
                val adjustment = preferencesRepository.getPaydayAdjustment()

                // 현재 급여 사이클 계산 (사용 현황 탭용)
                val currentPeriod = payPeriodCalculator.getCurrentPayPeriod(
                    payday = payday,
                    adjustment = adjustment
                )

                // 가장 오래된 거래 내역 확인하여 이전 버튼 활성화 여부 결정
                val oldestTransactionDate = getOldestTransactionDateUseCase()
                val canNavigatePrevious = if (oldestTransactionDate != null) {
                    // 가장 오래된 거래 내역이 속한 급여 기간 계산
                    val oldestPayPeriod = payPeriodCalculator.getCurrentPayPeriod(
                        currentDate = oldestTransactionDate,
                        payday = payday,
                        adjustment = adjustment
                    )
                    // 현재 기간이 가장 오래된 기간보다 이후면 이전으로 이동 가능
                    currentPeriod.startDate > oldestPayPeriod.startDate
                } else {
                    // 거래 내역이 없으면 이동 불가
                    false
                }

                uiState = uiState.copy(
                    currentPeriod = currentPeriod,
                    viewingPeriod = currentPeriod,  // 사용 현황 탭의 초기 기간
                    canNavigateNext = false,  // 현재 기간이므로 다음으로 이동 불가
                    canNavigatePrevious = canNavigatePrevious
                )

                // 초기 탭에 따라 데이터 로드
                // 기본 탭은 SETTINGS이므로 예산 템플릿만 로드
                loadBudgetTemplate(today)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    // 예산 템플릿 로드 (예산 설정 탭용)
    private fun loadBudgetTemplate(date: LocalDate) {
        budgetJob?.cancel()

        budgetJob = viewModelScope.launch {
            try {
                getCurrentBudgetPlanUseCase(date).collect { budgetPlan ->
                    if (budgetPlan != null) {
                        // 예산 템플릿이 있으면 급여와 카테고리 예산 로드
                        val formattedSalary = formatWithCommas(budgetPlan.monthlySalary.toLong())
                        uiState = uiState.copy(
                            monthlySalary = TextFieldValue(
                                text = formattedSalary,
                                selection = TextRange(formattedSalary.length)
                            )
                        )
                        loadCategoryBudgetsForTemplate(budgetPlan.id)
                    } else {
                        // 예산 템플릿이 없으면 빈 상태
                        uiState = uiState.copy(
                            monthlySalary = TextFieldValue(""),
                            categoryBudgets = emptyList(),
                            totalAllocated = 0.0,
                            unallocated = 0.0,
                            totalSpent = 0.0,
                            isLoading = false
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    // 사용 현황 탭에서 특정 기간의 지출 현황 로드
    private fun loadBudgetDataForPeriod(payPeriod: com.woojin.paymanagement.utils.PayPeriod) {
        budgetJob?.cancel()

        budgetJob = viewModelScope.launch {
            try {
                // 항상 현재(오늘) 유효한 예산 템플릿 사용
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                getCurrentBudgetPlanUseCase(today).collect { budgetPlan ->
                    if (budgetPlan != null) {
                        // 급여 정보 먼저 설정 (UI에 표시하기 위해)
                        val formattedSalary = formatWithCommas(budgetPlan.monthlySalary.toLong())
                        uiState = uiState.copy(
                            monthlySalary = TextFieldValue(
                                text = formattedSalary,
                                selection = TextRange(formattedSalary.length)
                            )
                        )

                        // 현재 템플릿으로 해당 기간의 지출 현황 계산
                        loadCategoryBudgetsForPeriod(budgetPlan, payPeriod)
                    } else {
                        // 예산 템플릿이 없으면 빈 상태
                        uiState = uiState.copy(
                            monthlySalary = TextFieldValue(""),
                            categoryBudgets = emptyList(),
                            totalAllocated = 0.0,
                            unallocated = 0.0,
                            totalSpent = 0.0,
                            isLoading = false
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    // 예산 설정 탭용: 템플릿만 로드 (지출 계산 안 함)
    private suspend fun loadCategoryBudgetsForTemplate(budgetPlanId: String) {
        getCategoryBudgetsUseCase(budgetPlanId).collect { categoryBudgets ->
            // 모든 지출/저축 카테고리 정보 가져오기
            val allCategories = getCategoriesUseCase(TransactionType.EXPENSE).first() +
                getCategoriesUseCase(TransactionType.SAVING).first()

            // 카테고리 정보만 포함 (지출 계산 안 함)
            val budgetsWithProgress = categoryBudgets.map { budget ->
                val categories = if (budget.isGroup) {
                    budget.categoryIds.mapNotNull { categoryId ->
                        allCategories.find { it.id == categoryId }
                    }
                } else {
                    emptyList()
                }

                CategoryBudgetWithProgress(
                    categoryBudget = budget,
                    spentAmount = 0.0,
                    remainingAmount = budget.allocatedAmount,
                    progress = 0f,
                    categories = categories,
                    categorySpentAmounts = emptyMap()
                )
            }

            val totalAllocated = categoryBudgets.sumOf { it.allocatedAmount }
            val salary = getSalaryAmount()

            uiState = uiState.copy(
                categoryBudgets = budgetsWithProgress,
                totalAllocated = totalAllocated,
                unallocated = salary - totalAllocated,
                totalSpent = 0.0,
                isLoading = false
            )
        }
    }

    // 사용 현황 탭용: 특정 기간의 지출 계산
    private suspend fun loadCategoryBudgetsForPeriod(
        budgetPlan: BudgetPlan,
        payPeriod: com.woojin.paymanagement.utils.PayPeriod
    ) {
        getCategoryBudgetsUseCase(budgetPlan.id).collect { categoryBudgets ->
            // 모든 지출/저축 카테고리 정보 가져오기
            val allCategories = getCategoriesUseCase(TransactionType.EXPENSE).first() +
                getCategoriesUseCase(TransactionType.SAVING).first()

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
                    // 단일 카테고리
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
            val salary = budgetPlan.monthlySalary  // BudgetPlan에서 직접 가져오기

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

        // 예산 설정 탭으로 전환 시, 항상 오늘 날짜의 템플릿 로드
        if (tab == BudgetTab.SETTINGS) {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            loadBudgetTemplate(today)
        }
        // 사용 현황 탭으로 전환 시, 현재 보고 있는 기간의 데이터 로드
        else if (tab == BudgetTab.PROGRESS) {
            val viewingPeriod = uiState.viewingPeriod
            if (viewingPeriod != null) {
                loadBudgetDataForPeriod(viewingPeriod)
            }
        }
    }

    fun toggleSalaryEditMode() {
        val isCurrentlyEditing = uiState.isSalaryEditing

        if (isCurrentlyEditing) {
            // 편집 모드를 끄면서 저장
            salaryUpdateJob?.cancel()
            saveMonthlySalaryChange()
        }

        uiState = uiState.copy(isSalaryEditing = !isCurrentlyEditing)
    }

    fun updateMonthlySalary(newValue: TextFieldValue) {
        // 쉼표 제거 후 숫자만 추출
        val digitsOnly = removeCommas(newValue.text)

        if (digitsOnly.isEmpty() || digitsOnly.matches(Regex("^\\d+$"))) {
            val formattedSalary = if (digitsOnly.isNotEmpty()) {
                val number = digitsOnly.toLongOrNull() ?: 0L
                formatWithCommas(number)
            } else {
                ""
            }

            // UI 업데이트
            uiState = uiState.copy(
                monthlySalary = TextFieldValue(
                    text = formattedSalary,
                    selection = TextRange(formattedSalary.length)
                ),
                unallocated = (digitsOnly.toDoubleOrNull() ?: 0.0) - uiState.totalAllocated
            )

            // 입력이 끝나면 자동 저장 (1초 debounce)
            salaryUpdateJob?.cancel()
            salaryUpdateJob = viewModelScope.launch {
                kotlinx.coroutines.delay(1000)  // 1초 대기
                saveMonthlySalaryChange()
            }
        }
    }

    // 급여 변경 저장 (예산 템플릿 생성 또는 업데이트)
    fun saveMonthlySalaryChange() {
        val salaryAmount = getSalaryAmount()
        if (salaryAmount <= 0) return

        viewModelScope.launch {
            try {
                uiState = uiState.copy(isSaving = true)

                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                // 현재 유효한 템플릿 가져오기
                val existingPlan = getCurrentBudgetPlanUseCase(today).first()

                if (existingPlan != null) {
                    // 기존 템플릿이 있으면 카테고리 예산을 백업
                    val existingCategoryBudgets = getCategoryBudgetsUseCase(existingPlan.id).first()

                    // 오늘 날짜의 템플릿이면 삭제 (중복 방지)
                    if (existingPlan.effectiveFromDate == today) {
                        deleteBudgetPlanUseCase(existingPlan.id)
                    }

                    // 새 예산 템플릿 생성
                    val newBudgetPlan = BudgetPlan(
                        id = uuid4().toString(),
                        effectiveFromDate = today,
                        monthlySalary = salaryAmount,
                        createdAt = today
                    )
                    saveBudgetPlanUseCase(newBudgetPlan)

                    // 기존 카테고리 예산을 새 템플릿으로 복사
                    existingCategoryBudgets.forEach { budget ->
                        val newBudget = CategoryBudget(
                            id = uuid4().toString(),
                            budgetPlanId = newBudgetPlan.id,
                            categoryIds = budget.categoryIds,
                            categoryName = budget.categoryName,
                            categoryEmoji = budget.categoryEmoji,
                            allocatedAmount = budget.allocatedAmount,
                            memo = budget.memo
                        )
                        saveCategoryBudgetUseCase(newBudget)
                    }
                } else {
                    // 템플릿이 없으면 새로 생성
                    val newBudgetPlan = BudgetPlan(
                        id = uuid4().toString(),
                        effectiveFromDate = today,
                        monthlySalary = salaryAmount,
                        createdAt = today
                    )
                    saveBudgetPlanUseCase(newBudgetPlan)
                }

                // 템플릿 재로드
                loadBudgetTemplate(today)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            } finally {
                uiState = uiState.copy(isSaving = false)
            }
        }
    }

    fun showAddCategoryDialog() {
        viewModelScope.launch {
            // 현재 예산이 설정되지 않은 지출/저축 카테고리 조회
            val expenseCategories = getCategoriesUseCase(TransactionType.EXPENSE).first()
            val savingCategories = getCategoriesUseCase(TransactionType.SAVING).first()
            val allCategories = expenseCategories + savingCategories
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

        if (amount <= 0) return

        viewModelScope.launch {
            try {
                uiState = uiState.copy(isSaving = true)

                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                // 현재 유효한 예산 템플릿 가져오기, 없으면 생성
                var budgetPlan = getCurrentBudgetPlanUseCase(today).first()
                if (budgetPlan == null) {
                    // 급여 정보가 없으면 예산 템플릿을 생성할 수 없음
                    val salaryAmount = getSalaryAmount()
                    if (salaryAmount <= 0) {
                        uiState = uiState.copy(error = "먼저 월 급여를 입력해주세요")
                        return@launch
                    }

                    budgetPlan = BudgetPlan(
                        id = uuid4().toString(),
                        effectiveFromDate = today,
                        monthlySalary = salaryAmount,
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            } finally {
                uiState = uiState.copy(isSaving = false)
            }
        }
    }

    fun showEditDialog(budget: CategoryBudgetWithProgress) {
        viewModelScope.launch {
            val expenseCategories = getCategoriesUseCase(TransactionType.EXPENSE).first()
            val savingCategories = getCategoriesUseCase(TransactionType.SAVING).first()
            val allCategories = expenseCategories + savingCategories

            // 다른 예산 아이템이 사용 중인 카테고리 IDs (현재 편집 대상 제외)
            val otherUsedCategoryIds = uiState.categoryBudgets
                .filter { it.categoryBudget.id != budget.categoryBudget.id }
                .flatMap { it.categoryBudget.categoryIds }
                .toSet()

            // 현재 편집 중인 카테고리 + 아직 미배정 카테고리
            val editAvailableCategories = allCategories.filter { it.id !in otherUsedCategoryIds }

            // 현재 이미 선택된 카테고리 (편집 대상 budget의 카테고리)
            val editSelectedCategories = allCategories
                .filter { it.id in budget.categoryBudget.categoryIds }
                .toSet()

            val formattedAmount = formatWithCommas(budget.categoryBudget.allocatedAmount.toLong())
            uiState = uiState.copy(
                showEditDialog = true,
                editingBudget = budget,
                editAmount = TextFieldValue(
                    text = formattedAmount,
                    selection = TextRange(formattedAmount.length)
                ),
                editMemo = budget.categoryBudget.memo ?: "",
                editAvailableCategories = editAvailableCategories,
                editSelectedCategories = editSelectedCategories,
                editGroupName = if (budget.categoryBudget.isGroup) budget.categoryBudget.categoryName else ""
            )
        }
    }

    fun hideEditDialog() {
        uiState = uiState.copy(
            showEditDialog = false,
            editingBudget = null,
            editAmount = TextFieldValue(""),
            editMemo = "",
            editAvailableCategories = emptyList(),
            editSelectedCategories = emptySet(),
            editGroupName = ""
        )
    }

    fun toggleEditCategorySelection(category: Category) {
        val current = uiState.editSelectedCategories
        val newSelection = if (category in current) current - category else current + category
        uiState = uiState.copy(editSelectedCategories = newSelection)
    }

    fun updateEditGroupName(name: String) {
        uiState = uiState.copy(editGroupName = name)
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

        val selectedCategories = uiState.editSelectedCategories
        if (selectedCategories.isEmpty()) return

        viewModelScope.launch {
            try {
                uiState = uiState.copy(isSaving = true)
                val memo = uiState.editMemo.ifBlank { null }
                val categoryIds = selectedCategories.map { it.id }
                val categoryName = if (selectedCategories.size == 1) {
                    selectedCategories.first().name
                } else {
                    uiState.editGroupName.ifBlank {
                        selectedCategories.joinToString(", ") { it.name }
                    }
                }
                val categoryEmoji = if (selectedCategories.size == 1) {
                    selectedCategories.first().emoji
                } else {
                    "📦"
                }
                updateCategoryBudgetUseCase(
                    id = editing.categoryBudget.id,
                    allocatedAmount = amount,
                    memo = memo,
                    categoryIds = categoryIds,
                    categoryName = categoryName,
                    categoryEmoji = categoryEmoji
                )
                hideEditDialog()
            } catch (e: CancellationException) {
                throw e
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
            } catch (e: CancellationException) {
                throw e
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

    fun navigateToPreviousPeriod() {
        val currentViewingPeriod = uiState.viewingPeriod ?: return
        val payday = preferencesRepository.getPayday()
        val adjustment = preferencesRepository.getPaydayAdjustment()

        viewModelScope.launch {
            val previousPeriod = payPeriodCalculator.getPreviousPayPeriod(
                currentPeriod = currentViewingPeriod,
                payday = payday,
                adjustment = adjustment
            )

            // 가장 오래된 거래 내역이 속한 급여 기간 확인
            val oldestTransactionDate = getOldestTransactionDateUseCase()
            // 이동한 후의 이전 기간이 가장 오래된 기간과 비교하여 더 이전으로 갈 수 있는지 확인
            val canNavigatePrevious = if (oldestTransactionDate != null) {
                // 가장 오래된 거래 내역이 속한 급여 기간 계산
                val oldestPayPeriod = payPeriodCalculator.getCurrentPayPeriod(
                    currentDate = oldestTransactionDate,
                    payday = payday,
                    adjustment = adjustment
                )
                // 이동한 이전 기간이 가장 오래된 기간보다 이후면 또 이전으로 이동 가능
                previousPeriod.startDate > oldestPayPeriod.startDate
            } else {
                // 거래 내역이 없으면 더 이상 이동 불가
                false
            }

            uiState = uiState.copy(
                viewingPeriod = previousPeriod,
                canNavigateNext = true,  // 이전으로 갔으므로 다음으로 이동 가능
                canNavigatePrevious = canNavigatePrevious
            )

            // 이전 기간의 지출 현황 로드
            loadBudgetDataForPeriod(previousPeriod)
        }
    }

    fun navigateToNextPeriod() {
        val currentViewingPeriod = uiState.viewingPeriod ?: return
        val currentPeriod = uiState.currentPeriod ?: return
        val payday = preferencesRepository.getPayday()
        val adjustment = preferencesRepository.getPaydayAdjustment()

        viewModelScope.launch {
            val nextPeriod = payPeriodCalculator.getNextPayPeriod(
                currentPeriod = currentViewingPeriod,
                payday = payday,
                adjustment = adjustment
            )

            // 가장 오래된 거래 내역이 속한 급여 기간 확인
            val oldestTransactionDate = getOldestTransactionDateUseCase()
            val canNavigatePrevious = if (oldestTransactionDate != null) {
                // 가장 오래된 거래 내역이 속한 급여 기간 계산
                val oldestPayPeriod = payPeriodCalculator.getCurrentPayPeriod(
                    currentDate = oldestTransactionDate,
                    payday = payday,
                    adjustment = adjustment
                )
                // 다음 기간이 가장 오래된 기간보다 이후면 이전으로 이동 가능
                nextPeriod.startDate > oldestPayPeriod.startDate
            } else {
                // 거래 내역이 없으면 이동 불가
                false
            }

            // 이동한 기간이 현재 기간인지 확인 (현재 기간이면 더 이상 미래로 못 감)
            val isCurrentPeriod = nextPeriod.startDate == currentPeriod.startDate

            uiState = uiState.copy(
                viewingPeriod = nextPeriod,
                canNavigateNext = !isCurrentPeriod,  // 현재 기간이 아니면 더 이동 가능
                canNavigatePrevious = canNavigatePrevious
            )

            // 다음 기간의 지출 현황 로드
            loadBudgetDataForPeriod(nextPeriod)
        }
    }

    private fun getSalaryAmount(): Double {
        return removeCommas(uiState.monthlySalary.text).toDoubleOrNull() ?: 0.0
    }
}
