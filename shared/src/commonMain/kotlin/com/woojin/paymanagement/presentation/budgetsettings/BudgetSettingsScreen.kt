package com.woojin.paymanagement.presentation.budgetsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.woojin.paymanagement.data.Category
import com.woojin.paymanagement.utils.Utils
import com.woojin.paymanagement.utils.PlatformBackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    viewModel: BudgetSettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit
) {
    val uiState = viewModel.uiState

    // 시스템 뒤로가기 버튼 처리
    PlatformBackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("예산 관리") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
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
                    text = { Text("예산 설정") }
                )
                Tab(
                    selected = uiState.selectedTab == BudgetTab.PROGRESS,
                    onClick = { viewModel.selectTab(BudgetTab.PROGRESS) },
                    text = { Text("사용 현황") }
                )
            }

            // 탭 내용
            when (uiState.selectedTab) {
                BudgetTab.SETTINGS -> BudgetSettingsTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToCategoryManagement = onNavigateToCategoryManagement
                )
                BudgetTab.PROGRESS -> BudgetProgressTab(
                    uiState = uiState,
                    viewModel = viewModel
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
                onConfirm = { viewModel.addCategoryBudget() }
            )
        }

        if (uiState.showEditDialog) {
            EditCategoryBudgetDialog(
                uiState = uiState,
                onDismiss = { viewModel.hideEditDialog() },
                onAmountChanged = { viewModel.updateEditAmount(it) },
                onMemoChanged = { viewModel.updateEditMemo(it) },
                onConfirm = { viewModel.updateCategoryBudget() }
            )
        }

        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("오류") },
                text = { Text(uiState.error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("확인")
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
    onNavigateToCategoryManagement: () -> Unit
) {
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
                                text = "💵 급여",
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
                                        contentDescription = "급여 수정",
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
                                placeholder = { Text("급여를 입력하세요") },
                                suffix = { Text("원") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.toggleSalaryEditMode() }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "완료",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        } else {
                            // 표시 모드: Text 표시
                            val salaryText = if (uiState.monthlySalary.text.isEmpty()) {
                                "급여를 설정하세요"
                            } else {
                                "${uiState.monthlySalary.text}원"
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
                            text = "📝 지출 계획",
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
                                text = "계획 합계:",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${Utils.formatAmount(uiState.totalAllocated)}원",
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
                                text = "남은 금액:",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${Utils.formatAmount(uiState.unallocated)}원",
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

        // 카테고리별 예산 목록
        items(uiState.categoryBudgets) { budget ->
            CategoryBudgetCard(
                budget = budget,
                onEdit = { viewModel.showEditDialog(budget) },
                onDelete = { viewModel.deleteCategoryBudget(budget) }
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
                            text = "+ 카테고리 추가",
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
                            text = "카테고리 관리",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetProgressTab(
    uiState: BudgetSettingsUiState,
    viewModel: BudgetSettingsViewModel
) {
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
                            text = "💰 전체 진행도",
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
                                text = "급여:",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val salaryAmount = uiState.monthlySalary.text.replace(",", "").toDoubleOrNull() ?: 0.0
                            Text(
                                text = "${Utils.formatAmount(salaryAmount)}원",
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
                                text = "예산:",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${Utils.formatAmount(uiState.totalAllocated)}원",
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
                                text = "사용:",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val percentage = if (uiState.totalAllocated > 0) {
                                ((uiState.totalSpent / uiState.totalAllocated) * 100).toInt()
                            } else 0
                            Text(
                                text = "${Utils.formatAmount(uiState.totalSpent)}원 ($percentage%)",
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
                                text = "남음:",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${Utils.formatAmount(uiState.totalAllocated - uiState.totalSpent)}원",
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

        // 카테고리별 진행도
        items(uiState.categoryBudgets) { budget ->
            CategoryProgressCard(budget = budget)
        }

        if (uiState.categoryBudgets.isEmpty()) {
            item {
                Text(
                    text = "예산이 설정된 카테고리가 없습니다.\n\"예산 설정\" 탭에서 카테고리를 추가해주세요.",
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
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                                text = "${Utils.formatAmount(budget.categoryBudget.allocatedAmount)}원",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "수정", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "삭제", tint = MaterialTheme.colorScheme.error)
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
                                text = "포함된 카테고리:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            budget.categories.forEach { category ->
                                Text(
                                    text = "  └─ ${category.emoji} ${category.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
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
                                    text = "메모",
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
    budget: CategoryBudgetWithProgress
) {
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
                    Text(
                        text = budget.categoryBudget.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "예산:",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${Utils.formatAmount(budget.categoryBudget.allocatedAmount)}원",
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
                        text = "사용:",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val percentage = (budget.progress * 100).toInt()
                    Text(
                        text = "${Utils.formatAmount(budget.spentAmount)}원 ($percentage%)",
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
                        text = if (budget.isOverBudget) "초과:" else "남음:",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${Utils.formatAmount(kotlin.math.abs(budget.remainingAmount))}원",
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
                                text = "카테고리별 지출:",
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
                                        text = "  ├─ ${category.emoji} ${category.name}:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${Utils.formatAmount(spent)}원",
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
                                    text = "  └─ 총:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${Utils.formatAmount(budget.spentAmount)}원",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = progressColor
                                )
                            }
                        }
                    }
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
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("카테고리 예산 추가") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                item {
                    Text(
                        text = "카테고리 선택 (복수 선택 가능)",
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
                            text = "추가할 수 있는 카테고리가 없습니다.",
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
                                    Text(
                                        text = category.emoji,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
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
                                text = "그룹명 입력",
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
                            label = { Text("그룹명") },
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
                        text = "배분 금액",
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
                        placeholder = { Text("금액을 입력하세요") },
                        suffix = { Text("원") },
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
                    Text(
                        text = "메모 (선택사항)",
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
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
fun EditCategoryBudgetDialog(
    uiState: BudgetSettingsUiState,
    onDismiss: () -> Unit,
    onAmountChanged: (TextFieldValue) -> Unit,
    onMemoChanged: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val budget = uiState.editingBudget ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${budget.categoryBudget.categoryEmoji} ${budget.categoryBudget.categoryName} 예산 수정") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = "배분 금액",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.editAmount,
                    onValueChange = onAmountChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("금액을 입력하세요") },
                    suffix = { Text("원") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "메모 (선택사항)",
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
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = uiState.editAmount.text.isNotEmpty()
            ) {
                Text("수정")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
