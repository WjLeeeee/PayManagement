package com.woojin.paymanagement.presentation.recurringtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.RecurringPattern
import com.woojin.paymanagement.data.RecurringTransaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.strings.AppStrings
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.PlatformBackHandler
import com.woojin.paymanagement.utils.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionScreen(
    viewModel: RecurringTransactionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: (RecurringTransaction) -> Unit
) {
    val strings = LocalStrings.current
    val uiState = viewModel.uiState

    // Android 뒤로가기 버튼 처리
    PlatformBackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.recurringTransactionManagement) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, strings.goBack)
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // 오늘 실행할 항목 제외한 나머지 항목들
            val otherTransactions = uiState.recurringTransactions.filter { transaction ->
                !uiState.todayTransactions.any { it.id == transaction.id }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 자동 실행 토글 카드
                item {
                    AutoExecuteToggleCard(
                        isEnabled = uiState.isAutoExecuteEnabled,
                        onToggle = { viewModel.toggleAutoExecute() }
                    )
                }

                // 오늘 실행할 항목 섹션
                if (uiState.todayTransactions.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.todayItems,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(uiState.todayTransactions) { transaction ->
                        RecurringTransactionItem(
                            transaction = transaction,
                            isHighlighted = true,
                            categories = uiState.categories,
                            onEdit = { viewModel.showEditDialog(transaction) },
                            onDelete = { viewModel.deleteRecurringTransaction(transaction.id) },
                            onToggleActive = { viewModel.toggleActive(transaction) },
                            onClick = { onNavigateToAddTransaction(transaction) }
                        )
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                // 반복 거래 추가 버튼
                item {
                    AddRecurringTransactionItem(
                        onClick = { viewModel.showAddDialog() }
                    )
                }

                // 빈 상태 표시
                if (uiState.recurringTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.noRegisteredRecurringTransactions,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 오늘 실행할 항목 제외한 나머지 항목들
                items(otherTransactions) { transaction ->
                    RecurringTransactionItem(
                        transaction = transaction,
                        isHighlighted = false,
                        categories = uiState.categories,
                        onEdit = { viewModel.showEditDialog(transaction) },
                        onDelete = { viewModel.deleteRecurringTransaction(transaction.id) },
                        onToggleActive = { viewModel.toggleActive(transaction) },
                        onClick = null
                    )
                }
            }
        }

        // 반복 거래 추가/수정 다이얼로그
        if (uiState.showAddDialog) {
            RecurringTransactionDialog(
                transaction = uiState.editingTransaction,
                categories = uiState.categories,
                customPaymentMethods = uiState.customPaymentMethods,
                onDismiss = { viewModel.hideDialog() },
                onSave = { transaction ->
                    viewModel.saveRecurringTransaction(transaction)
                }
            )
        }
    }
}

@Composable
private fun AutoExecuteToggleCard(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.recurringAutoExecute,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.recurringAutoExecuteDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun AddRecurringTransactionItem(
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        onClick = onClick,
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.add,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.addRecurringTransaction,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun RecurringTransactionItem(
    transaction: RecurringTransaction,
    isHighlighted: Boolean,
    categories: List<com.woojin.paymanagement.data.Category>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    onClick: (() -> Unit)?
) {
    val strings = LocalStrings.current
    val categoryEmoji = categories.firstOrNull { it.name == transaction.category }?.emoji ?: "📝"
    val containerColors = if (isHighlighted) {
        listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHighlighted) 6.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(colors = containerColors)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 첫 번째 줄: 카테고리 이모지, 이름, 토글, 편집, 삭제
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = categoryEmoji,
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Column {
                            Text(
                                text = if (transaction.type == TransactionType.SAVING && transaction.merchant.isBlank())
                                    transaction.category
                                else
                                    transaction.merchant,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = transaction.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 활성화 토글
                        Switch(
                            checked = transaction.isActive,
                            onCheckedChange = { onToggleActive() },
                            modifier = Modifier.height(32.dp)
                        )

                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = strings.edit,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = strings.delete,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 두 번째 줄: 금액과 반복 패턴
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 금액
                    Text(
                        text = when (transaction.type) {
                            TransactionType.INCOME -> "+${strings.amountWithUnit(Utils.formatAmount(transaction.amount))}"
                            TransactionType.EXPENSE -> strings.amountWithUnit(Utils.formatAmount(transaction.amount))
                            TransactionType.SAVING -> strings.amountWithUnit(Utils.formatAmount(transaction.amount))
                            TransactionType.INVESTMENT -> strings.amountWithUnit(Utils.formatAmount(transaction.amount))
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (transaction.type) {
                            TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                            TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                            TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                            TransactionType.INVESTMENT -> com.woojin.paymanagement.theme.InvestmentColor.color
                        }
                    )

                    // 반복 패턴 표시
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = getPatternText(transaction, strings),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // 결제 수단
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.paymentMethodDisplay(getPaymentMethodDisplayName(transaction.paymentMethod, strings)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 오늘 실행할 항목에 대한 안내
                if (isHighlighted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = strings.tapToAddTransaction,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getPatternText(transaction: RecurringTransaction, strings: AppStrings): String {
    return when (transaction.pattern) {
        RecurringPattern.MONTHLY -> {
            val day = transaction.dayOfMonth ?: 1
            strings.recurringDayOfMonth(day)
        }
        RecurringPattern.WEEKLY -> {
            val dayName = when (transaction.dayOfWeek) {
                1 -> strings.monday
                2 -> strings.tuesday
                3 -> strings.wednesday
                4 -> strings.thursday
                5 -> strings.friday
                6 -> strings.saturday
                7 -> strings.sunday
                else -> "?"
            }
            strings.recurringDayOfWeek(dayName)
        }
    }
}

private fun getPaymentMethodDisplayName(paymentMethod: com.woojin.paymanagement.data.PaymentMethod, strings: AppStrings): String {
    return when (paymentMethod) {
        com.woojin.paymanagement.data.PaymentMethod.CASH -> strings.cashCheckCard
        com.woojin.paymanagement.data.PaymentMethod.CARD -> strings.creditCard
        com.woojin.paymanagement.data.PaymentMethod.BALANCE_CARD -> strings.balanceCard
        com.woojin.paymanagement.data.PaymentMethod.GIFT_CARD -> strings.giftCard
    }
}
