package com.woojin.paymanagement.presentation.budgetsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.BudgetItem
import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.data.TransferItem
import com.woojin.paymanagement.utils.Utils
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.PlatformBackHandler
import com.woojin.paymanagement.utils.removeCommas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    viewModel: BudgetSettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit,
    nativeAdContent: (@Composable () -> Unit)? = null  // null이면 광고 없음 (로드 실패·광고 제거 구매 포함)
) {
    val strings = LocalStrings.current
    val uiState = viewModel.uiState

    // 시스템 뒤로가기 버튼 처리
    PlatformBackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.budgetManagement) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, strings.goBack)
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 탭
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                Tab(
                    selected = uiState.selectedTab == BudgetTab.SETTINGS,
                    onClick = { viewModel.selectTab(BudgetTab.SETTINGS) },
                    text = { Text(strings.budgetSettings) }
                )
                Tab(
                    selected = uiState.selectedTab == BudgetTab.PROGRESS,
                    onClick = { viewModel.selectTab(BudgetTab.PROGRESS) },
                    text = { Text(strings.usageStatus) }
                )
            }

            // 탭 내용
            when (uiState.selectedTab) {
                BudgetTab.SETTINGS -> BudgetSettingsTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToCategoryManagement = onNavigateToCategoryManagement,
                    nativeAdContent = nativeAdContent
                )
                BudgetTab.PROGRESS -> BudgetProgressTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    nativeAdContent = nativeAdContent
                )
            }
        }

        // 다이얼로그들
        if (uiState.showAddCategoryDialog) {
            AddCategoryBudgetDialog(
                uiState = uiState,
                onDismiss = { viewModel.hideAddCategoryDialog() },
                onCategoryToggled = { viewModel.toggleCategorySelection(it) },
                onGroupNameChanged = { viewModel.updateGroupName(it) },
                onAmountChanged = { viewModel.updateNewBudgetAmount(it) },
                onMemoChanged = { viewModel.updateNewBudgetMemo(it) },
                itemsActions = BudgetItemsEditorActions(
                    onAddItem = { viewModel.addNewBudgetItem() },
                    onNameChanged = { id, name -> viewModel.updateNewBudgetItemName(id, name) },
                    onAmountChanged = { id, value -> viewModel.updateNewBudgetItemAmount(id, value) },
                    onRemoveItem = { viewModel.removeNewBudgetItem(it) },
                    onApplyTotal = { viewModel.applyNewBudgetItemsTotal() }
                ),
                onAccountToggled = { viewModel.toggleNewBudgetAccount(it) },
                onAccountAmountChanged = { id, value -> viewModel.updateNewBudgetAccountAmount(id, value) },
                onConfirm = { viewModel.addCategoryBudget() }
            )
        }

        if (uiState.showEditDialog) {
            EditCategoryBudgetDialog(
                uiState = uiState,
                onDismiss = { viewModel.hideEditDialog() },
                onCategoryToggled = { viewModel.toggleEditCategorySelection(it) },
                onGroupNameChanged = { viewModel.updateEditGroupName(it) },
                onAmountChanged = { viewModel.updateEditAmount(it) },
                onMemoChanged = { viewModel.updateEditMemo(it) },
                itemsActions = BudgetItemsEditorActions(
                    onAddItem = { viewModel.addEditItem() },
                    onNameChanged = { id, name -> viewModel.updateEditItemName(id, name) },
                    onAmountChanged = { id, value -> viewModel.updateEditItemAmount(id, value) },
                    onRemoveItem = { viewModel.removeEditItem(it) },
                    onApplyTotal = { viewModel.applyEditItemsTotal() }
                ),
                onAccountToggled = { viewModel.toggleEditAccount(it) },
                onAccountAmountChanged = { id, value -> viewModel.updateEditAccountAmount(id, value) },
                onConfirm = { viewModel.updateCategoryBudget() }
            )
        }

        if (uiState.showTransferPlanSheet) {
            TransferPlanSheet(
                uiState = uiState,
                onDismiss = { viewModel.hideTransferPlanSheet() },
                onStartEditing = { viewModel.startTransferPlanEditing() },
                onCancelEditing = { viewModel.cancelTransferPlanEditing() },
                editorActions = BudgetItemsEditorActions(
                    onAddItem = { viewModel.addTransferDraft() },
                    onNameChanged = { id, name -> viewModel.updateTransferDraftName(id, name) },
                    onAmountChanged = { id, value -> viewModel.updateTransferDraftAmount(id, value) },
                    onRemoveItem = { viewModel.removeTransferDraft(it) },
                    onApplyTotal = {}
                ),
                onSave = { viewModel.saveTransferPlan() }
            )
        }

        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text(strings.error) },
                text = { Text(uiState.error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(strings.confirm)
                    }
                }
            )
        }
    }
}

@Composable
fun BudgetSettingsTab(
    uiState: BudgetSettingsUiState,
    viewModel: BudgetSettingsViewModel,
    onNavigateToCategoryManagement: () -> Unit,
    nativeAdContent: (@Composable () -> Unit)? = null
) {
    val strings = LocalStrings.current
    var budgetToDelete by remember { mutableStateOf<CategoryBudgetWithProgress?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 고정 급여 입력
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💵 ${strings.monthlySalary}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // 편집 모드가 아닐 때만 수정 버튼 표시
                            if (!uiState.isSalaryEditing) {
                                IconButton(
                                    onClick = { viewModel.toggleSalaryEditMode() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = strings.editSalary,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.isSalaryEditing) {
                            // 편집 모드: TextField 표시
                            OutlinedTextField(
                                value = uiState.monthlySalary,
                                onValueChange = { viewModel.updateMonthlySalary(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(strings.enterSalary) },
                                suffix = { Text(strings.currencySymbol) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.toggleSalaryEditMode() }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = strings.done,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        } else {
                            // 표시 모드: Text 표시
                            val salaryText = if (uiState.monthlySalary.text.isEmpty()) {
                                strings.setSalary
                            } else {
                                strings.amountWithUnit(uiState.monthlySalary.text)
                            }

                            Text(
                                text = salaryText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.monthlySalary.text.isEmpty()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }

                        // 월급날 이체 계획 요약 (탭하면 바텀시트)
                        TransferPlanSummaryRow(
                            transfers = uiState.transfers,
                            salary = removeCommas(uiState.monthlySalary.text).toDoubleOrNull() ?: 0.0,
                            onClick = { viewModel.showTransferPlanSheet() }
                        )
                    }
                }
            }
        }

        // 계획 합계
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📝 ${strings.spendingPlan}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.plannedTotal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.amountWithUnit(Utils.formatAmount(uiState.totalAllocated)),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.remainingLabel,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.amountWithUnit(Utils.formatAmount(uiState.unallocated)),
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.unallocated < 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
            }
        }

        // 네이티브 광고
        if (nativeAdContent != null) {
            item { nativeAdContent() }
        }

        // 카테고리별 예산 목록
        items(uiState.categoryBudgets) { budget ->
            CategoryBudgetCard(
                budget = budget,
                accountSummary = uiState.accountSummaryOf(budget.categoryBudget),
                onEdit = { viewModel.showEditDialog(budget) },
                onDelete = { budgetToDelete = budget }
            )
        }

        // 카테고리 추가 버튼
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.showAddCategoryDialog() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.addCategoryLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 카테고리 관리 진입점
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCategoryManagement() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.categoryManagement,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // 카테고리 예산 삭제 확인 다이얼로그
    budgetToDelete?.let { budget ->
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text(strings.deleteBudget) },
            text = { Text(strings.deleteBudgetConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategoryBudget(budget)
                        budgetToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun BudgetProgressTab(
    uiState: BudgetSettingsUiState,
    viewModel: BudgetSettingsViewModel,
    nativeAdContent: (@Composable () -> Unit)? = null
) {
    val strings = LocalStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 급여 사이클 with 이전/다음 버튼
        item {
            uiState.viewingPeriod?.let { period ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 이전 버튼 (가장 오래된 거래 내역 기간이면 비활성화)
                    IconButton(
                        onClick = { viewModel.navigateToPreviousPeriod() },
                        enabled = uiState.canNavigatePrevious
                    ) {
                        Text(
                            text = "◀",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (uiState.canNavigatePrevious) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            }
                        )
                    }

                    // 급여 기간 표시
                    Text(
                        text = "📅 ${period.displayText}",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    // 다음 버튼 (미래 기간이면 비활성화)
                    IconButton(
                        onClick = { viewModel.navigateToNextPeriod() },
                        enabled = uiState.canNavigateNext
                    ) {
                        Text(
                            text = "▶",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (uiState.canNavigateNext) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            }
                        )
                    }
                }
            }
        }

        // 이체 체크 (보고 있는 급여 기간)
        if (uiState.transfers.isNotEmpty()) {
            item {
                TransferCheckCard(
                    transfers = uiState.transfers,
                    checkedIds = uiState.transferChecks,
                    onToggle = { viewModel.toggleTransferCheck(it) }
                )
            }
        }

        // 전체 진행도
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "💰 ${strings.overallProgress}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 급여
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.salaryLabel,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val salaryAmount = uiState.monthlySalary.text.replace(",", "").toDoubleOrNull() ?: 0.0
                            Text(
                                text = strings.amountWithUnit(Utils.formatAmount(salaryAmount)),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.budgetLabel,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.amountWithUnit(Utils.formatAmount(uiState.totalAllocated)),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.usedLabel,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val percentage = if (uiState.totalAllocated > 0) {
                                ((uiState.totalSpent / uiState.totalAllocated) * 100).toInt()
                            } else 0
                            Text(
                                text = "${strings.amountWithUnit(Utils.formatAmount(uiState.totalSpent))} ($percentage%)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = if (uiState.totalAllocated > 0) {
                                (uiState.totalSpent / uiState.totalAllocated).toFloat()
                            } else 0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.remainingLabel,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.amountWithUnit(Utils.formatAmount(uiState.totalAllocated - uiState.totalSpent)),
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.totalSpent > uiState.totalAllocated) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
            }
        }

        // 네이티브 광고
        if (nativeAdContent != null) {
            item { nativeAdContent() }
        }

        // 카테고리별 진행도
        items(uiState.categoryBudgets) { budget ->
            CategoryProgressCard(
                budget = budget,
                accountSummary = uiState.accountSummaryOf(budget.categoryBudget)
            )
        }

        if (uiState.categoryBudgets.isEmpty()) {
            item {
                Text(
                    text = strings.noBudgetCategoriesMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryBudgetCard(
    budget: CategoryBudgetWithProgress,
    accountSummary: List<Pair<String, Double?>>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 메인 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = budget.categoryBudget.categoryEmoji,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = budget.categoryBudget.categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.amountWithUnit(Utils.formatAmount(budget.categoryBudget.allocatedAmount)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (accountSummary.isNotEmpty()) {
                                Text(
                                    text = accountSummaryText(accountSummary, strings),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, strings.edit, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, strings.delete, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // 그룹인 경우 하위 카테고리 표시
                if (budget.categoryBudget.isGroup && budget.categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = strings.includedCategories,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            budget.categories.forEach { category ->
                                Text(
                                    text = "  └─ ${if (category.emoji.isNotBlank()) "${category.emoji} " else ""}${category.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 세부 항목 표시
                if (budget.categoryBudget.items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BudgetItemsSummary(items = budget.categoryBudget.items)
                }

                // 메모 표시
                budget.categoryBudget.memo?.let { memo ->
                    if (memo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = strings.memo,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = memo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryProgressCard(
    budget: CategoryBudgetWithProgress,
    accountSummary: List<Pair<String, Double?>>
) {
    val strings = LocalStrings.current
    val progressColor = when {
        budget.progress < 0.7f -> MaterialTheme.colorScheme.primary
        budget.progress < 0.9f -> MaterialTheme.colorScheme.tertiary
        budget.progress < 1.0f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = budget.categoryBudget.categoryEmoji,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = budget.categoryBudget.categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (accountSummary.isNotEmpty()) {
                            Text(
                                text = accountSummaryText(accountSummary, strings),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.budgetLabel,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(budget.categoryBudget.allocatedAmount)),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.usedLabel,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val percentage = (budget.progress * 100).toInt()
                    Text(
                        text = "${strings.amountWithUnit(Utils.formatAmount(budget.spentAmount))} ($percentage%)",
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = budget.progress.coerceAtMost(1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = progressColor
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (budget.isOverBudget) strings.exceededLabel else strings.remainingLabel,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(budget.remainingAmount))),
                        fontWeight = FontWeight.Bold,
                        color = if (budget.isOverBudget) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }

                // 그룹인 경우 하위 카테고리별 지출 표시
                if (budget.categoryBudget.isGroup && budget.categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = strings.categorySpending,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            budget.categories.forEach { category ->
                                val spent = budget.categorySpentAmounts[category.id] ?: 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "  ├─ ${if (category.emoji.isNotBlank()) "${category.emoji} " else ""}${category.name}:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = strings.amountWithUnit(Utils.formatAmount(spent)),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "  └─ ${strings.totalLabel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.amountWithUnit(Utils.formatAmount(budget.spentAmount)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = progressColor
                                )
                            }
                        }
                    }
                }

                // 세부 항목 표시
                if (budget.categoryBudget.items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BudgetItemsSummary(items = budget.categoryBudget.items)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCategoryBudgetDialog(
    uiState: BudgetSettingsUiState,
    onDismiss: () -> Unit,
    onCategoryToggled: (Category) -> Unit,
    onGroupNameChanged: (String) -> Unit,
    onAmountChanged: (TextFieldValue) -> Unit,
    onMemoChanged: (String) -> Unit,
    itemsActions: BudgetItemsEditorActions,
    onAccountToggled: (String) -> Unit,
    onAccountAmountChanged: (String, TextFieldValue) -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addCategoryBudget) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                item {
                    Text(
                        text = strings.selectCategoriesMultiple,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 카테고리 칩 그리드
                item {
                    if (uiState.availableCategories.isEmpty()) {
                        Text(
                            text = strings.noCategoriesAvailable,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.availableCategories.forEach { category ->
                                val isSelected = category in uiState.selectedCategories
                                val backgroundColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                                val borderColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                }
                                val textColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Row(
                                    modifier = Modifier
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .background(
                                            color = backgroundColor,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable { onCategoryToggled(category) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (category.emoji.isNotBlank()) {
                                        Text(
                                            text = category.emoji,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }

                // 여러 카테고리 선택 시 그룹명 입력
                if (uiState.selectedCategories.size > 1) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📦",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = strings.groupNameInput,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                            value = uiState.groupName,
                            onValueChange = onGroupNameChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(strings.groupNameLabel) },
                            placeholder = {
                                Text(uiState.selectedCategories.joinToString(", ") { it.name })
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Text(
                        text = strings.allocatedAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = uiState.newBudgetAmount,
                        onValueChange = onAmountChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(strings.enterAmountPlaceholder) },
                        suffix = { Text(strings.currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    BudgetItemsEditor(
                        items = uiState.newBudgetItems,
                        compareAmountText = uiState.newBudgetAmount.text,
                        labels = budgetItemsEditorLabels(strings),
                        actions = itemsActions
                    )
                }

                item {
                    if (uiState.transfers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        BudgetAccountSelector(
                            transfers = uiState.transfers,
                            accounts = uiState.newBudgetAccounts,
                            budgetAmountText = uiState.newBudgetAmount.text,
                            onToggle = onAccountToggled,
                            onAmountChanged = onAccountAmountChanged
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Text(
                        text = strings.memoOptional,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = uiState.newBudgetMemo,
                        onValueChange = onMemoChanged,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = uiState.selectedCategories.isNotEmpty() && uiState.newBudgetAmount.text.isNotEmpty()
            ) {
                Text(strings.add)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditCategoryBudgetDialog(
    uiState: BudgetSettingsUiState,
    onDismiss: () -> Unit,
    onCategoryToggled: (Category) -> Unit,
    onGroupNameChanged: (String) -> Unit,
    onAmountChanged: (TextFieldValue) -> Unit,
    onMemoChanged: (String) -> Unit,
    itemsActions: BudgetItemsEditorActions,
    onAccountToggled: (String) -> Unit,
    onAccountAmountChanged: (String, TextFieldValue) -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalStrings.current
    uiState.editingBudget ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editBudgetTitle(
            if (uiState.editSelectedCategories.size == 1) uiState.editSelectedCategories.first().emoji
            else uiState.editingBudget?.categoryBudget?.categoryEmoji ?: "",
            if (uiState.editSelectedCategories.size == 1) uiState.editSelectedCategories.first().name
            else uiState.editGroupName.ifBlank { uiState.editingBudget?.categoryBudget?.categoryName ?: "" }
        )) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                // 카테고리 선택
                item {
                    Text(
                        text = strings.selectCategoriesMultiple,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.editAvailableCategories.forEach { category ->
                            val isSelected = category in uiState.editSelectedCategories
                            val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface

                            Row(
                                modifier = Modifier
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .background(color = backgroundColor, shape = RoundedCornerShape(20.dp))
                                    .clickable { onCategoryToggled(category) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (category.emoji.isNotBlank()) {
                                    Text(text = category.emoji, fontSize = 16.sp)
                                }
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                // 여러 카테고리 선택 시 그룹명 입력
                if (uiState.editSelectedCategories.size > 1) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.groupNameLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.editGroupName,
                            onValueChange = onGroupNameChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(strings.groupNameLabel) },
                            placeholder = {
                                Text(uiState.editSelectedCategories.joinToString(", ") { it.name })
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // 배분 금액
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.allocatedAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.editAmount,
                        onValueChange = onAmountChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(strings.enterAmountPlaceholder) },
                        suffix = { Text(strings.currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // 세부 항목
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    BudgetItemsEditor(
                        items = uiState.editItems,
                        compareAmountText = uiState.editAmount.text,
                        labels = budgetItemsEditorLabels(strings),
                        actions = itemsActions
                    )
                }

                // 사용 통장
                item {
                    if (uiState.transfers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        BudgetAccountSelector(
                            transfers = uiState.transfers,
                            accounts = uiState.editAccounts,
                            budgetAmountText = uiState.editAmount.text,
                            onToggle = onAccountToggled,
                            onAmountChanged = onAccountAmountChanged
                        )
                    }
                }

                // 메모
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.memoOptional,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.editMemo,
                        onValueChange = onMemoChanged,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = uiState.editSelectedCategories.isNotEmpty() && uiState.editAmount.text.isNotEmpty()
            ) {
                Text(strings.edit)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

/**
 * 세부 항목 편집 콜백 모음 (추가/수정 다이얼로그 공용)
 */
class BudgetItemsEditorActions(
    val onAddItem: () -> Unit,
    val onNameChanged: (itemId: String, name: String) -> Unit,
    val onAmountChanged: (itemId: String, value: TextFieldValue) -> Unit,
    val onRemoveItem: (itemId: String) -> Unit,
    val onApplyTotal: () -> Unit
)

/**
 * 이름·금액 행 편집기의 문구 (세부 항목 / 이체 계획 공용)
 */
class ItemsEditorLabels(
    val title: String,
    val namePlaceholder: String,
    val totalLabel: String,
    val exceedText: (String) -> String,
    val remainderText: (String) -> String,
    val applyTotalLabel: String?  // null이면 "합계 적용" 버튼 숨김
)

fun budgetItemsEditorLabels(strings: com.woojin.paymanagement.strings.AppStrings) = ItemsEditorLabels(
    title = strings.budgetItemsOptional,
    namePlaceholder = strings.budgetItemName,
    totalLabel = strings.budgetItemsTotal,
    exceedText = strings::itemsExceedBudget,
    remainderText = strings::itemsRemainder,
    applyTotalLabel = strings.applyItemsTotal
)

/**
 * 이름·금액 행 목록 + 추가 버튼 + 합계/기준 금액 비교 편집기
 */
@Composable
fun BudgetItemsEditor(
    items: List<BudgetItemDraft>,
    compareAmountText: String,
    labels: ItemsEditorLabels,
    actions: BudgetItemsEditorActions
) {
    val strings = LocalStrings.current
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = labels.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { actions.onNameChanged(item.id, it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(labels.namePlaceholder) },
                    singleLine = true,
                    colors = fieldColors
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = item.amount,
                    onValueChange = { actions.onAmountChanged(item.id, it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("0") },
                    suffix = { Text(strings.currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors
                )
                IconButton(onClick = { actions.onRemoveItem(item.id) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = strings.delete,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        TextButton(onClick = actions.onAddItem) {
            Text(strings.addBudgetItem)
        }

        if (items.isNotEmpty()) {
            val total = items.totalAmount
            val budgetAmount = removeCommas(compareAmountText).toDoubleOrNull() ?: 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = labels.totalLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = strings.amountWithUnit(Utils.formatAmount(total)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (total > 0 && total != budgetAmount) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val diffText = when {
                        total > budgetAmount -> labels.exceedText(Utils.formatAmount(total - budgetAmount))
                        else -> labels.remainderText(Utils.formatAmount(budgetAmount - total))
                    }
                    Text(
                        text = diffText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (total > budgetAmount) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (labels.applyTotalLabel != null) {
                        TextButton(onClick = actions.onApplyTotal) {
                            Text(labels.applyTotalLabel)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 카드에 표시하는 세부 항목 목록 (읽기 전용)
 */
@Composable
fun BudgetItemsSummary(items: List<BudgetItem>) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = strings.budgetItems,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "  · ${item.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(item.amount)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "  ${strings.budgetItemsTotal}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.amountWithUnit(Utils.formatAmount(items.sumOf { it.amount })),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** 카드 표시용 통장 요약: 하나면 "ISA", 여럿이면 "ISA 400,000원 · 증권 200,000원" */
private fun accountSummaryText(
    summary: List<Pair<String, Double?>>,
    strings: com.woojin.paymanagement.strings.AppStrings
): String = summary.joinToString(" · ") { (name, amount) ->
    if (amount == null) name else "$name ${strings.amountWithUnit(Utils.formatAmount(amount))}"
}

/**
 * 예산 다이얼로그의 사용 통장 선택 (복수 선택, 다시 누르면 해제)
 * 통장을 2개 이상 고르면 통장별 배분 금액 입력란과 예산 대비 합계를 보여준다
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetAccountSelector(
    transfers: List<TransferItem>,
    accounts: List<AccountDraft>,
    budgetAmountText: String,
    onToggle: (String) -> Unit,
    onAmountChanged: (String, TextFieldValue) -> Unit
) {
    val strings = LocalStrings.current
    val selectedIds = accounts.map { it.transferItemId }.toSet()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = strings.budgetAccountOptional,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            transfers.forEach { transfer ->
                FilterChip(
                    selected = transfer.id in selectedIds,
                    onClick = { onToggle(transfer.id) },
                    label = { Text(transfer.accountName) }
                )
            }
        }

        if (accounts.size >= 2) {
            Spacer(modifier = Modifier.height(8.dp))
            // 이체 계획 순서대로 표시
            transfers.filter { it.id in selectedIds }.forEach { transfer ->
                val draft = accounts.first { it.transferItemId == transfer.id }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transfer.accountName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.amount,
                        onValueChange = { onAmountChanged(transfer.id, it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("0") },
                        suffix = { Text(strings.currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val total = accounts.sumOf { it.amountValue }
            val budgetAmount = removeCommas(budgetAmountText).toDoubleOrNull() ?: 0.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.accountSplitTotal,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = strings.amountWithUnit(Utils.formatAmount(total)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (budgetAmount > 0 && total != budgetAmount) {
                Text(
                    text = if (total > budgetAmount) {
                        strings.accountSplitExceeds(Utils.formatAmount(total - budgetAmount))
                    } else {
                        strings.accountSplitUnassigned(Utils.formatAmount(budgetAmount - total))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (total > budgetAmount) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 사용 현황 탭: 보고 있는 급여 기간의 이체 완료 체크 카드
 */
@Composable
fun TransferCheckCard(
    transfers: List<TransferItem>,
    checkedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    val strings = LocalStrings.current
    val doneCount = transfers.count { it.id in checkedIds }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💳 ${strings.transferCheck}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.transferCheckProgress(doneCount, transfers.size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (doneCount == transfers.size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                transfers.forEach { transfer ->
                    val checked = transfer.id in checkedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(transfer.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onToggle(transfer.id) }
                        )
                        Text(
                            text = transfer.accountName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = strings.amountWithUnit(Utils.formatAmount(transfer.amount)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * 급여 카드 하단: 이체 계획 한 줄 요약. 계획이 없으면 설정 안내
 */
@Composable
fun TransferPlanSummaryRow(
    transfers: List<TransferItem>,
    salary: Double,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (transfers.isEmpty()) {
            Text(
                text = strings.setupTransferPlan,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        } else {
            val total = transfers.sumOf { it.amount }
            val leftover = salary - total
            Text(
                text = strings.transferPlanShort,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.transferPlanSummary(transfers.size, Utils.formatAmount(total)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (salary > 0 && leftover != 0.0) {
                    Text(
                        text = if (leftover < 0) {
                            strings.transferExceedsSalary(Utils.formatAmount(-leftover))
                        } else {
                            strings.leftoverAfterTransfer(Utils.formatAmount(leftover))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (leftover < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "▸",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 월급날 이체 계획 바텀시트: 보기 모드(통장별 이체액 + 연결 예산 비교) / 편집 모드
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferPlanSheet(
    uiState: BudgetSettingsUiState,
    onDismiss: () -> Unit,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    editorActions: BudgetItemsEditorActions,
    onSave: () -> Unit
) {
    val strings = LocalStrings.current
    val salary = removeCommas(uiState.monthlySalary.text).toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            if (uiState.isTransferPlanEditing) {
                BudgetItemsEditor(
                    items = uiState.transferDrafts,
                    compareAmountText = uiState.monthlySalary.text,
                    labels = ItemsEditorLabels(
                        title = strings.transferPlan,
                        namePlaceholder = strings.accountName,
                        totalLabel = strings.transferTotal,
                        exceedText = strings::transferExceedsSalary,
                        remainderText = strings::leftoverAfterTransfer,
                        applyTotalLabel = null
                    ),
                    actions = editorActions
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancelEditing) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSave, enabled = !uiState.isSaving) {
                        Text(strings.save)
                    }
                }
                return@Column
            }

            // ----- 보기 모드 -----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.transferPlan,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onStartEditing) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = strings.editTransferPlan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.edit)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            uiState.transfers.forEach { transfer ->
                val linkedBudget = uiState.categoryBudgets.sumOf { it.categoryBudget.amountForAccount(transfer.id) }
                val diff = transfer.amount - linkedBudget
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transfer.accountName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.amountWithUnit(Utils.formatAmount(transfer.amount)),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (linkedBudget > 0) {
                            val diffText = when {
                                diff < 0 -> " · ${strings.shortageAmount(Utils.formatAmount(-diff))}"
                                diff > 0 -> " · ${strings.itemsRemainder(Utils.formatAmount(diff))}"
                                else -> ""
                            }
                            Text(
                                text = strings.linkedBudgetAmount(Utils.formatAmount(linkedBudget)) + diffText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (diff < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            val total = uiState.transfers.sumOf { it.amount }
            val leftover = salary - total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.transferTotal,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(total)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (salary > 0 && leftover != 0.0) {
                        Text(
                            text = if (leftover < 0) {
                                strings.transferExceedsSalary(Utils.formatAmount(-leftover))
                            } else {
                                strings.leftoverAfterTransfer(Utils.formatAmount(leftover))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (leftover < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
