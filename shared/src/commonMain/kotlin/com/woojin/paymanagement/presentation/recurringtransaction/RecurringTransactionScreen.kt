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
import com.woojin.paymanagement.utils.PlatformBackHandler
import com.woojin.paymanagement.utils.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionScreen(
    viewModel: RecurringTransactionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: (RecurringTransaction) -> Unit
) {
    val uiState = viewModel.uiState

    // Android 뒤로가기 버튼 처리
    PlatformBackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("반복 거래 관리") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
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
                // 오늘 실행할 항목 섹션
                if (uiState.todayTransactions.isNotEmpty()) {
                    item {
                        Text(
                            text = "오늘 실행할 항목",
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
                                text = "등록된 반복 거래가 없습니다",
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
                onDismiss = { viewModel.hideDialog() },
                onSave = { transaction ->
                    viewModel.saveRecurringTransaction(transaction)
                }
            )
        }
    }
}

@Composable
private fun AddRecurringTransactionItem(
    onClick: () -> Unit
) {
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
                    contentDescription = "추가",
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "반복 거래 추가",
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
                                text = transaction.merchant,
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
                                contentDescription = "수정",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "삭제",
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
                            TransactionType.INCOME -> "+${Utils.formatAmount(transaction.amount)}원"
                            TransactionType.EXPENSE -> "${Utils.formatAmount(transaction.amount)}원"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (transaction.type) {
                            TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                            TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                        }
                    )

                    // 반복 패턴 표시
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = getPatternText(transaction),
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
                    text = "결제수단: ${getPaymentMethodDisplayName(transaction.paymentMethod)}",
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
                            text = "탭하여 거래 추가하기",
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

private fun getPatternText(transaction: RecurringTransaction): String {
    return when (transaction.pattern) {
        RecurringPattern.MONTHLY -> {
            val day = transaction.dayOfMonth ?: 1
            "매달 ${day}일"
        }
        RecurringPattern.WEEKLY -> {
            val dayName = when (transaction.dayOfWeek) {
                1 -> "월요일"
                2 -> "화요일"
                3 -> "수요일"
                4 -> "목요일"
                5 -> "금요일"
                6 -> "토요일"
                7 -> "일요일"
                else -> "?"
            }
            "매주 $dayName"
        }
    }
}

private fun getPaymentMethodDisplayName(paymentMethod: com.woojin.paymanagement.data.PaymentMethod): String {
    return when (paymentMethod) {
        com.woojin.paymanagement.data.PaymentMethod.CASH -> "현금/체크카드"
        com.woojin.paymanagement.data.PaymentMethod.CARD -> "신용카드"
        com.woojin.paymanagement.data.PaymentMethod.BALANCE_CARD -> "잔액권"
        com.woojin.paymanagement.data.PaymentMethod.GIFT_CARD -> "상품권"
    }
}
