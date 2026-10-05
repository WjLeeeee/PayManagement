package com.woojin.paymanagement.presentation.budgetsettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.rotate
import kotlinx.coroutines.delay
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.woojin.paymanagement.theme.BrandColor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woojin.paymanagement.presentation.addtransaction.SelectablePillChip
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
            // 탭 (세그먼트 컨트롤)
            BudgetTabSelector(
                selectedTab = uiState.selectedTab,
                onSelect = { viewModel.selectTab(it) }
            )

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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 월급 + 이체 계획 + 지출 계획 (민트 카드 하나로 통합)
        item {
            BrandHeroCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💵 ${strings.monthlySalary}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    // 편집 모드가 아닐 때만 수정 버튼 표시
                    if (!uiState.isSalaryEditing) {
                        IconButton(
                            onClick = { viewModel.toggleSalaryEditMode() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = strings.editSalary,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

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
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color(0xFF191F28),
                            unfocusedTextColor = Color(0xFF191F28),
                            cursorColor = BrandColor.mint
                        ),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleSalaryEditMode() }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = strings.done,
                                    tint = BrandColor.mint
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
                        fontSize = if (uiState.monthlySalary.text.isEmpty()) 18.sp else 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (uiState.monthlySalary.text.isEmpty()) Color.White.copy(alpha = 0.8f) else Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 반투명 영역: 이체 계획 / 지출 계획
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp)
                ) {
                    // 월급날 이체 계획 요약 (탭하면 바텀시트)
                    TransferPlanSummaryRow(
                        transfers = uiState.transfers,
                        salary = removeCommas(uiState.monthlySalary.text).toDoubleOrNull() ?: 0.0,
                        onClick = { viewModel.showTransferPlanSheet() }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                    // 지출 계획 합계 / 남음
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.spendingPlan,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.width(HeroLabelWidth)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${strings.plannedTotal.noColon()} ${strings.amountWithUnit(Utils.formatAmount(uiState.totalAllocated))}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = "${strings.remainingLabel.noColon()} ${strings.amountWithUnit(Utils.formatAmount(uiState.unallocated))}",
                                fontSize = 11.sp,
                                fontWeight = if (uiState.unallocated < 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.unallocated < 0) HeroWarningColor else Color.White.copy(alpha = 0.85f)
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

        // 섹션 제목: 지출 계획 N개 + 카테고리 관리
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.spendingPlan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (uiState.categoryBudgets.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${uiState.categoryBudgets.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // 카테고리 관리 진입점
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToCategoryManagement() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = BrandColor.mint
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.categoryManagement,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandColor.mint
                    )
                }
            }
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

        // 카테고리 추가 버튼 (민트 점선)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandColor.mint.copy(alpha = 0.05f))
                    .dashedBorder(BrandColor.mint, 14.dp)
                    .clickable { viewModel.showAddCategoryDialog() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.addCategoryLabel,
                    fontSize = 14.5.sp,
                    color = BrandColor.mint,
                    fontWeight = FontWeight.ExtraBold
                )
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 급여 사이클 with 이전/다음 버튼
        item {
            uiState.viewingPeriod?.let { period ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 이전 버튼 (가장 오래된 거래 내역 기간이면 비활성화)
                    IconButton(
                        onClick = { viewModel.navigateToPreviousPeriod() },
                        enabled = uiState.canNavigatePrevious
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = if (uiState.canNavigatePrevious) BrandColor.mint
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }

                    // 급여 기간 표시
                    Text(
                        text = "📅 ${period.displayText}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    // 다음 버튼 (미래 기간이면 비활성화)
                    IconButton(
                        onClick = { viewModel.navigateToNextPeriod() },
                        enabled = uiState.canNavigateNext
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = if (uiState.canNavigateNext) BrandColor.mint
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
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

        // 전체 진행도 (민트 카드)
        item {
            val percentage = if (uiState.totalAllocated > 0) {
                ((uiState.totalSpent / uiState.totalAllocated) * 100).toInt()
            } else 0
            val progress = if (uiState.totalAllocated > 0) {
                (uiState.totalSpent / uiState.totalAllocated).toFloat()
            } else 0f
            val salaryAmount = uiState.monthlySalary.text.replace(",", "").toDoubleOrNull() ?: 0.0
            val remaining = uiState.totalAllocated - uiState.totalSpent

            BrandHeroCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💰 ${strings.overallProgress}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(text = "$percentage%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = strings.usedLabel.noColon(), fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                Text(
                    text = strings.amountWithUnit(Utils.formatAmount(uiState.totalSpent)),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                ProgressBar(
                    progress = progress,
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(vertical = 9.dp)
                ) {
                    HeroStat(strings.salaryLabel.noColon(), Utils.formatAmount(salaryAmount), Modifier.weight(1f))
                    HeroStat(strings.budgetLabel.noColon(), Utils.formatAmount(uiState.totalAllocated), Modifier.weight(1f))
                    HeroStat(
                        label = strings.remainingLabel.noColon(),
                        value = Utils.formatAmount(remaining),
                        modifier = Modifier.weight(1f),
                        valueColor = if (uiState.totalSpent > uiState.totalAllocated) HeroWarningColor else Color.White
                    )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryBudgetCard(
    budget: CategoryBudgetWithProgress,
    accountSummary: List<Pair<String, Double?>>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    ListCard {
        // 메인 헤더: 아이콘 타일 + 이름/통장 + 금액/수정·삭제
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiTile(budget.categoryBudget.categoryEmoji)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = budget.categoryBudget.categoryName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (accountSummary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = accountSummaryText(accountSummary, strings),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = strings.amountWithUnit(Utils.formatAmount(budget.categoryBudget.allocatedAmount)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            strings.edit,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            strings.delete,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 그룹인 경우 하위 카테고리를 칩으로 표시
        if (budget.categoryBudget.isGroup && budget.categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                budget.categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (category.emoji.isNotBlank()) {
                            Text(text = category.emoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = category.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 세부 항목 표시
        if (budget.categoryBudget.items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            BudgetItemsSummary(items = budget.categoryBudget.items)
        }

        // 메모 표시
        budget.categoryBudget.memo?.let { memo ->
            if (memo.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                InnerBox {
                    InnerBoxLabel(strings.memo)
                    Text(
                        text = memo,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
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
    // 구간은 기존과 동일 (70% / 90% / 100%), 색만 브랜드 톤으로
    val progressColor = when {
        budget.progress < 0.7f -> BrandColor.mint
        budget.progress < 0.9f -> ProgressGold
        budget.progress < 1.0f -> ProgressOrange
        else -> MaterialTheme.colorScheme.error
    }
    val percentage = (budget.progress * 100).toInt()
    val reachedLimit = budget.progress >= 1.0f

    ListCard {
        // 헤더: 아이콘 타일 + 이름/통장 + 퍼센트 배지
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiTile(budget.categoryBudget.categoryEmoji)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = budget.categoryBudget.categoryName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (accountSummary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = accountSummaryText(accountSummary, strings),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(progressColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = "$percentage%", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = progressColor)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 사용 / 예산 · 남음(초과)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = strings.amountWithUnit(Utils.formatAmount(budget.spentAmount)),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (reachedLimit) progressColor else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "/ ${strings.amountWithUnit(Utils.formatAmount(budget.categoryBudget.allocatedAmount))}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${(if (budget.isOverBudget) strings.exceededLabel else strings.remainingLabel).noColon()} ${
                    strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(budget.remainingAmount)))
                }",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (budget.isOverBudget) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        ProgressBar(progress = budget.progress, color = progressColor)

        // 그룹인 경우 하위 카테고리별 지출 표시
        if (budget.categoryBudget.isGroup && budget.categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            InnerBox {
                InnerBoxLabel(strings.categorySpending.noColon())
                budget.categories.forEach { category ->
                    val spent = budget.categorySpentAmounts[category.id] ?: 0.0
                    val isZero = spent == 0.0
                    val rowColor = if (isZero) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.emoji,
                            fontSize = 12.5.sp,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            text = category.name,
                            fontSize = 12.5.sp,
                            color = rowColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = strings.amountWithUnit(Utils.formatAmount(spent)),
                            fontSize = 12.5.sp,
                            fontWeight = if (isZero) FontWeight.Normal else FontWeight.SemiBold,
                            color = rowColor
                        )
                    }
                }
                InnerBoxDivider()
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.totalLabel.noColon(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(budget.spentAmount)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = progressColor
                    )
                }
            }
        }

        // 세부 항목 표시
        if (budget.categoryBudget.items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            BudgetItemsSummary(items = budget.categoryBudget.items)
        }
    }
}

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
    CategoryBudgetFormDialog(
        title = strings.addCategoryBudget,
        confirmText = strings.add,
        canConfirm = uiState.selectedCategories.isNotEmpty() && uiState.newBudgetAmount.text.isNotEmpty(),
        availableCategories = uiState.availableCategories,
        selectedCategories = uiState.selectedCategories,
        groupNameTitle = strings.groupNameInput,
        groupName = uiState.groupName,
        amount = uiState.newBudgetAmount,
        items = uiState.newBudgetItems,
        transfers = uiState.transfers,
        accounts = uiState.newBudgetAccounts,
        memo = uiState.newBudgetMemo,
        onDismiss = onDismiss,
        onCategoryToggled = onCategoryToggled,
        onGroupNameChanged = onGroupNameChanged,
        onAmountChanged = onAmountChanged,
        onMemoChanged = onMemoChanged,
        itemsActions = itemsActions,
        onAccountToggled = onAccountToggled,
        onAccountAmountChanged = onAccountAmountChanged,
        onConfirm = onConfirm
    )
}

/**
 * 카테고리 예산 추가 / 수정 공용 폼 (전체 화면 다이얼로그 + 번호 섹션 카드)
 * 표시만 담당하고, 값과 동작은 모두 호출하는 쪽에서 넘겨받음
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryBudgetFormDialog(
    title: String,
    confirmText: String,
    canConfirm: Boolean,
    availableCategories: List<Category>,
    selectedCategories: Set<Category>,
    groupNameTitle: String,
    groupName: String,
    amount: TextFieldValue,
    items: List<BudgetItemDraft>,
    transfers: List<TransferItem>,
    accounts: List<AccountDraft>,
    memo: String,
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

    // 선택 섹션(③④⑤) 펼침 상태 - 화면 안에서만 기억. 내용이 있으면 펼친 채로 시작
    var itemsExpanded by remember { mutableStateOf(items.isNotEmpty()) }
    var accountsExpanded by remember { mutableStateOf(accounts.isNotEmpty()) }
    var memoExpanded by remember { mutableStateOf(memo.isNotBlank()) }

    // 다이얼로그 구조는 유지, 화면 전체를 덮도록 표시
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .imePadding()
            ) {
                // 상단 바: 왼쪽 X(취소) + 제목
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancel,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ① 카테고리 (필수)
                    FormSectionCard(number = 1, title = strings.selectCategoriesMultiple) {
                        if (availableCategories.isEmpty()) {
                            Text(
                                text = strings.noCategoriesAvailable,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                availableCategories.forEach { category ->
                                    SelectablePillChip(
                                        label = category.name,
                                        emoji = category.emoji.takeIf { it.isNotBlank() },
                                        selected = category in selectedCategories,
                                        onClick = { onCategoryToggled(category) }
                                    )
                                }
                            }
                        }

                        // 여러 카테고리 선택 시 그룹명 입력
                        AnimatedVisibility(
                            visible = selectedCategories.size > 1,
                            enter = expandVertically(tween(TransferAnimMs)) + fadeIn(tween(TransferAnimMs)),
                            exit = shrinkVertically(tween(TransferAnimMs)) + fadeOut(tween(TransferAnimMs / 2))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandColor.mint.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "📦 $groupNameTitle",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColor.mint
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FilledField(
                                    value = groupName,
                                    onValueChange = onGroupNameChanged,
                                    placeholder = selectedCategories.joinToString(", ") { it.name },
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }

                    // ② 배분 금액 (필수) - 큰 숫자 + 민트 밑줄
                    FormSectionCard(number = 2, title = strings.allocatedAmount) {
                        BasicTextField(
                            value = amount,
                            onValueChange = onAmountChanged,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(BrandColor.mint),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.weight(1f, fill = false)) {
                                            if (amount.text.isEmpty()) {
                                                Text(
                                                    text = "0",
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            }
                                            innerTextField()
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = strings.currencySymbol,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(BrandColor.mint)
                                    )
                                }
                            }
                        )
                    }

                    // ③ 세부 항목 (선택, 접기)
                    val itemsTotal = items.totalAmount
                    FormSectionCard(
                        number = 3,
                        title = strings.budgetItemsOptional,
                        optional = true,
                        expanded = itemsExpanded,
                        onToggle = { itemsExpanded = !itemsExpanded },
                        summary = if (itemsTotal > 0) strings.amountWithUnit(Utils.formatAmount(itemsTotal)) else null
                    ) {
                        BudgetItemsEditor(
                            items = items,
                            compareAmountText = amount.text,
                            labels = budgetItemsEditorLabels(strings),
                            actions = itemsActions,
                            showTitle = false
                        )
                    }

                    // ④ 사용 통장 (선택, 접기) - 이체 계획이 있을 때만
                    if (transfers.isNotEmpty()) {
                        val selectedNames = transfers
                            .filter { t -> accounts.any { it.transferItemId == t.id } }
                            .map { it.accountName }
                        FormSectionCard(
                            number = 4,
                            title = strings.budgetAccountOptional,
                            optional = true,
                            expanded = accountsExpanded,
                            onToggle = { accountsExpanded = !accountsExpanded },
                            summary = selectedNames.takeIf { it.isNotEmpty() }?.joinToString(", ")
                        ) {
                            BudgetAccountSelector(
                                transfers = transfers,
                                accounts = accounts,
                                budgetAmountText = amount.text,
                                onToggle = onAccountToggled,
                                onAmountChanged = onAccountAmountChanged,
                                showTitle = false
                            )
                        }
                    }

                    // ⑤ 메모 (선택, 접기)
                    FormSectionCard(
                        number = if (transfers.isNotEmpty()) 5 else 4,
                        title = strings.memoOptional,
                        optional = true,
                        expanded = memoExpanded,
                        onToggle = { memoExpanded = !memoExpanded },
                        summary = memo.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
                    ) {
                        FilledField(
                            value = memo,
                            onValueChange = onMemoChanged,
                            placeholder = "",
                            singleLine = false,
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }

                // 하단 저장 버튼
                Button(
                    onClick = onConfirm,
                    enabled = canConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandColor.mint,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(text = confirmText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 번호가 붙은 섹션 카드
 * - 필수(optional = false): 번호 민트, 항상 펼침
 * - 선택(optional = true): 번호 회색, 헤더를 눌러 접기/펼치기, 접혀 있을 때 요약 표시
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormSectionCard(
    number: Int,
    title: String,
    optional: Boolean = false,
    expanded: Boolean = true,
    onToggle: () -> Unit = {},
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(TransferAnimMs),
        label = "sectionArrow"
    )

    // 선택 섹션을 펼치면 펼쳐지는 만큼 같이 스크롤해서 카드 전체가 보이게 함
    // (맨 아래에서 메모를 펼쳤을 때 입력칸이 화면 밖으로 숨지 않도록)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(expanded) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }
        if (optional && expanded) {
            // 펼쳐지는 애니메이션 동안 높이가 계속 늘어나므로 끝날 때까지 따라가며 스크롤
            val steps = 6
            repeat(steps) {
                delay((TransferAnimMs / steps).toLong())
                bringIntoViewRequester.bringIntoView()
            }
            delay(40)
            bringIntoViewRequester.bringIntoView()
        }
    }

    ListCard(modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (optional) Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onToggle) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (optional) MaterialTheme.colorScheme.surfaceVariant else BrandColor.mint),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (optional) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (optional) {
                // 접혀 있을 때만 요약 표시
                if (!expanded && summary != null) {
                    Text(
                        text = summary,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 140.dp)
                            .padding(start = 8.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .rotate(arrowRotation)
                )
            }
        }
        AnimatedVisibility(
            visible = !optional || expanded,
            enter = expandVertically(tween(TransferAnimMs), expandFrom = Alignment.Top) + fadeIn(tween(TransferAnimMs)),
            exit = shrinkVertically(tween(TransferAnimMs), shrinkTowards = Alignment.Top) + fadeOut(tween(TransferAnimMs / 2))
        ) {
            Column(modifier = Modifier.padding(top = 14.dp), content = content)
        }
    }
}

/** 테두리 없는 회색 채움형 입력창 (포커스 시 민트 테두리) */
@Composable
private fun FilledField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else maxLines,
        shape = RoundedCornerShape(12.dp),
        colors = filledFieldColors(containerColor)
    )
}

@Composable
private fun filledFieldColors(containerColor: Color = MaterialTheme.colorScheme.surfaceVariant) =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        focusedBorderColor = BrandColor.mint,
        unfocusedBorderColor = Color.Transparent,
        cursorColor = BrandColor.mint
    )

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

    CategoryBudgetFormDialog(
        title = strings.editBudgetTitle(
            if (uiState.editSelectedCategories.size == 1) uiState.editSelectedCategories.first().emoji
            else uiState.editingBudget?.categoryBudget?.categoryEmoji ?: "",
            if (uiState.editSelectedCategories.size == 1) uiState.editSelectedCategories.first().name
            else uiState.editGroupName.ifBlank { uiState.editingBudget?.categoryBudget?.categoryName ?: "" }
        ),
        confirmText = strings.edit,
        canConfirm = uiState.editSelectedCategories.isNotEmpty() && uiState.editAmount.text.isNotEmpty(),
        availableCategories = uiState.editAvailableCategories,
        selectedCategories = uiState.editSelectedCategories,
        groupNameTitle = strings.groupNameLabel,
        groupName = uiState.editGroupName,
        amount = uiState.editAmount,
        items = uiState.editItems,
        transfers = uiState.transfers,
        accounts = uiState.editAccounts,
        memo = uiState.editMemo,
        onDismiss = onDismiss,
        onCategoryToggled = onCategoryToggled,
        onGroupNameChanged = onGroupNameChanged,
        onAmountChanged = onAmountChanged,
        onMemoChanged = onMemoChanged,
        itemsActions = itemsActions,
        onAccountToggled = onAccountToggled,
        onAccountAmountChanged = onAccountAmountChanged,
        onConfirm = onConfirm
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
    actions: BudgetItemsEditorActions,
    showTitle: Boolean = true  // 섹션 카드 안에서는 카드 제목을 쓰므로 숨김
) {
    val strings = LocalStrings.current
    val fieldColors = filledFieldColors()

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTitle) {
            Text(
                text = labels.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

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
                    shape = RoundedCornerShape(12.dp),
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
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )
                IconButton(onClick = { actions.onRemoveItem(item.id) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.delete,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        TextButton(
            onClick = actions.onAddItem,
            colors = ButtonDefaults.textButtonColors(contentColor = BrandColor.mint)
        ) {
            Text(strings.addBudgetItem, fontWeight = FontWeight.Bold)
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
                        TextButton(
                            onClick = actions.onApplyTotal,
                            colors = ButtonDefaults.textButtonColors(contentColor = BrandColor.mint)
                        ) {
                            Text(labels.applyTotalLabel, fontWeight = FontWeight.Bold)
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
    InnerBox {
        InnerBoxLabel(strings.budgetItems)
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = strings.amountWithUnit(Utils.formatAmount(item.amount)),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
        InnerBoxDivider()
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = strings.budgetItemsTotal,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = strings.amountWithUnit(Utils.formatAmount(items.sumOf { it.amount })),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
    onAmountChanged: (String, TextFieldValue) -> Unit,
    showTitle: Boolean = true  // 섹션 카드 안에서는 카드 제목을 쓰므로 숨김
) {
    val strings = LocalStrings.current
    val selectedIds = accounts.map { it.transferItemId }.toSet()
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTitle) {
            Text(
                text = strings.budgetAccountOptional,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            transfers.forEach { transfer ->
                SelectablePillChip(
                    label = transfer.accountName,
                    selected = transfer.id in selectedIds,
                    onClick = { onToggle(transfer.id) }
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
                        shape = RoundedCornerShape(12.dp),
                        colors = filledFieldColors()
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
    val allDone = doneCount == transfers.size

    // 펼침 상태 (화면 안에서만 기억, 저장 안 함)
    // - 처음 들어올 때: 전부 완료면 접힌 채로 시작
    // - 마지막 항목을 체크하면: 체크 표시를 잠깐 보여준 뒤 부드럽게 접힘
    // - 하나라도 체크를 풀면: 바로 다시 펼침
    var expanded by remember { mutableStateOf(!allDone) }
    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(allDone) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }
        if (allDone) {
            delay(TransferCollapseDelayMs)
            expanded = false
        } else {
            expanded = true
        }
    }
    val collapsed = allDone && !expanded

    // 화살표: 접힘 ⌄(0도) ↔ 펼침 ⌃(180도) 회전
    val arrowRotation by animateFloatAsState(
        targetValue = if (collapsed) 0f else 180f,
        animationSpec = tween(TransferAnimMs),
        label = "transferArrow"
    )

    ListCard {
        // ----- 헤더: 접힘/펼침 모두 똑같이 보임 (본문만 접히고 펼쳐짐) -----
        val iconBg by animateColorAsState(
            targetValue = if (allDone) BrandColor.mint else MaterialTheme.colorScheme.surfaceVariant,
            animationSpec = tween(TransferAnimMs),
            label = "transferIconBg"
        )
        val iconTint by animateColorAsState(
            targetValue = if (allDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            animationSpec = tween(TransferAnimMs),
            label = "transferIconTint"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    // 전부 완료된 상태에서만 눌러서 접기/펼치기
                    if (allDone) Modifier.clip(RoundedCornerShape(12.dp)).clickable { expanded = !expanded }
                    else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 상태 아이콘: 진행 중 = 회색, 전부 완료 = 민트 체크
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.transferCheck,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.transferCheckProgress(doneCount, transfers.size),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (allDone) BrandColor.mint else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = strings.transferPlanSummary(
                        transfers.size,
                        Utils.formatAmount(transfers.sumOf { it.amount })
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 접기/펼치기 화살표 (전부 완료일 때만, ⌄ ↔ ⌃ 회전)
            if (allDone) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .rotate(arrowRotation)
                )
            }
        }

        // ----- 본문: 진행 막대 + 체크 목록 (세로로 접히고 펼쳐짐) -----
        AnimatedVisibility(
            visible = !collapsed,
            enter = expandVertically(
                animationSpec = tween(TransferAnimMs),
                expandFrom = Alignment.Top
            ) + fadeIn(tween(TransferAnimMs)),
            exit = shrinkVertically(
                animationSpec = tween(TransferAnimMs),
                shrinkTowards = Alignment.Top
            ) + fadeOut(tween(TransferAnimMs / 2))
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                ProgressBar(
                    progress = if (transfers.isNotEmpty()) doneCount.toFloat() / transfers.size else 0f,
                    color = BrandColor.mint,
                    height = 6.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                transfers.forEachIndexed { index, transfer ->
                    val checked = transfer.id in checkedIds
                    if (index > 0) InnerBoxDivider(vertical = 0.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(transfer.id) }
                            .padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 체크 박스 (민트)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (checked) BrandColor.mint else Color.Transparent)
                                .border(
                                    width = if (checked) 0.dp else 1.5.dp,
                                    color = if (checked) Color.Transparent else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(7.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = transfer.accountName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (checked) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = strings.amountWithUnit(Utils.formatAmount(transfer.amount)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/** 이체 체크 접기/펼치기 애니메이션 시간 */
private const val TransferAnimMs = 280
/** 마지막 체크 후 접히기까지 잠깐 기다리는 시간 (체크 표시를 볼 수 있게) */
private const val TransferCollapseDelayMs = 450L

/**
 * 급여 카드(민트) 안의 이체 계획 한 줄 요약. 계획이 없으면 설정 안내
 */
@Composable
fun TransferPlanSummaryRow(
    transfers: List<TransferItem>,
    salary: Double,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (transfers.isEmpty()) {
            Text(
                text = strings.setupTransferPlan,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        } else {
            val total = transfers.sumOf { it.amount }
            val leftover = salary - total
            Text(
                text = strings.transferPlanShort,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.width(HeroLabelWidth)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.transferPlanSummary(transfers.size, Utils.formatAmount(total)),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                if (salary > 0 && leftover != 0.0) {
                    Text(
                        text = if (leftover < 0) {
                            strings.transferExceedsSalary(Utils.formatAmount(-leftover))
                        } else {
                            strings.leftoverAfterTransfer(Utils.formatAmount(leftover))
                        },
                        fontSize = 11.sp,
                        fontWeight = if (leftover < 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (leftover < 0) HeroWarningColor else Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onStartEditing,
                    colors = ButtonDefaults.textButtonColors(contentColor = BrandColor.mint)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = strings.editTransferPlan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.edit, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            uiState.transfers.forEachIndexed { index, transfer ->
                val linkedBudget = uiState.categoryBudgets.sumOf { it.categoryBudget.amountForAccount(transfer.id) }
                val diff = transfer.amount - linkedBudget
                if (index > 0) InnerBoxDivider(vertical = 0.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 통장 첫 글자 타일
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(BrandColor.mint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = transfer.accountName.take(1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandColor.mint
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = transfer.accountName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.amountWithUnit(Utils.formatAmount(transfer.amount)),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
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
                                fontSize = 11.sp,
                                color = if (diff < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val total = uiState.transfers.sumOf { it.amount }
            val leftover = salary - total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.transferTotal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(total)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (salary > 0 && leftover != 0.0) {
                        Text(
                            text = if (leftover < 0) {
                                strings.transferExceedsSalary(Utils.formatAmount(-leftover))
                            } else {
                                strings.leftoverAfterTransfer(Utils.formatAmount(leftover))
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (leftover < 0) MaterialTheme.colorScheme.error else BrandColor.mint
                        )
                    }
                }
            }
        }
    }
}

// ===================== 예산 관리 화면 공통 디자인 요소 =====================

private val HeroLabelWidth = 62.dp
/** 민트 카드 위 경고(초과·부족) 글자색 */
private val HeroWarningColor = Color(0xFFFFD6D6)
private val ProgressGold = Color(0xFFE9A23B)
private val ProgressOrange = Color(0xFFFF9800)

/** "계획 합계:" 같은 라벨 끝의 콜론 제거 (표시용) */
private fun String.noColon(): String = trimEnd().removeSuffix(":").trimEnd()

/** 예산 설정 / 사용 현황 탭 선택 (세그먼트 컨트롤) */
@Composable
private fun BudgetTabSelector(
    selectedTab: BudgetTab,
    onSelect: (BudgetTab) -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        listOf(BudgetTab.SETTINGS to strings.budgetSettings, BudgetTab.PROGRESS to strings.usageStatus)
            .forEach { (tab, label) ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (isSelected) Modifier.shadow(1.dp, RoundedCornerShape(10.dp)) else Modifier)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) BrandColor.mint else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
    }
}

/** 민트 그라데이션 강조 카드 */
@Composable
private fun BrandHeroCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(BrandColor.mint, Color(0xFF47B49C))))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        content = content
    )
}

/** 흰 카드 + 얇은 테두리 (목록 항목용) */
@Composable
private fun ListCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content
    )
}

/** 카드 안 회색 상자 (세부 항목 · 메모 · 카테고리별 지출) */
@Composable
private fun InnerBox(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        content = content
    )
}

@Composable
private fun InnerBoxLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun InnerBoxDivider(vertical: Dp = 6.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = vertical)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}

/** 42dp 이모지 타일 */
@Composable
private fun EmojiTile(emoji: String) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 21.sp)
    }
}

/** 둥근 진행 막대 (progress는 0~1로 잘라서 표시) */
@Composable
private fun ProgressBar(
    progress: Float,
    color: Color,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    height: Dp = 8.dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        val fraction = progress.coerceIn(0f, 1f)
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

/** 민트 카드 하단 3칸 요약의 한 칸 */
@Composable
private fun HeroStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, maxLines = 1)
    }
}

/** 점선 테두리 */
private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp): Modifier = this.drawBehind {
    val stroke = 1.5.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    )
}
