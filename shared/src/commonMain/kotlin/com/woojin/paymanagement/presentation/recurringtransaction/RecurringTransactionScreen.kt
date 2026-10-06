package com.woojin.paymanagement.presentation.recurringtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.woojin.paymanagement.theme.BrandColor
import com.woojin.paymanagement.theme.InvestmentColor
import com.woojin.paymanagement.theme.SavingColor
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var transactionToDelete by remember { mutableStateOf<RecurringTransaction?>(null) }

    // Android 뒤로가기 버튼 처리
    PlatformBackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            // 헤더: ← + 제목 (다른 화면과 동일)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.goBack,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = strings.recurringTransactionManagement,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandColor.mint,
                            modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 6.dp)
                        )
                    }

                    items(uiState.todayTransactions) { transaction ->
                        RecurringTransactionItem(
                            transaction = transaction,
                            isHighlighted = true,
                            categories = uiState.categories,
                            onEdit = { viewModel.showEditDialog(transaction) },
                            onDelete = { transactionToDelete = transaction },
                            onToggleActive = { viewModel.toggleActive(transaction) },
                            onClick = { onNavigateToAddTransaction(transaction) }
                        )
                    }

                    item {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            RecurringSectionBand()
                        }
                    }
                }

                // 반복 거래 추가 버튼
                item {
                    AddRecurringTransactionItem(
                        count = otherTransactions.size,
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
                        onDelete = { transactionToDelete = transaction },
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

        // 반복 거래 삭제 확인 다이얼로그
        transactionToDelete?.let { transaction ->
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text(strings.deleteRecurringTransaction) },
                text = { Text(strings.deleteRecurringTransactionConfirm) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteRecurringTransaction(transaction.id)
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(strings.delete)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToDelete = null }) {
                        Text(strings.cancel)
                    }
                }
            )
        }
    }
}

/**
 * 화면 좌우 끝까지 이어지는 회색 구분 띠 (메인 화면과 동일)
 */
@Composable
private fun RecurringSectionBand(horizontalBleed: Dp = 16.dp) {
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val extra = horizontalBleed.roundToPx() * 2
                val width = constraints.maxWidth + extra
                val placeable = measurable.measure(
                    constraints.copy(minWidth = width, maxWidth = width)
                )
                layout(constraints.maxWidth, placeable.height) {
                    placeable.place(-extra / 2, 0)
                }
            }
            .height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun mintSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = BrandColor.mint,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
    uncheckedBorderColor = Color.Transparent
)

@Composable
private fun AutoExecuteToggleCard(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.recurringAutoExecute,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = strings.recurringAutoExecuteDescription,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = mintSwitchColors()
        )
    }
}

/**
 * 목록 제목 "반복 거래 N" + 오른쪽 민트 "+ 반복 거래 추가"
 */
@Composable
private fun AddRecurringTransactionItem(
    count: Int,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 0.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings.recurringTransactions,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = BrandColor.mint,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = strings.addRecurringTransaction,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrandColor.mint
            )
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
    val typeColor = when (transaction.type) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.SAVING -> SavingColor.color
        TransactionType.INVESTMENT -> InvestmentColor.color
    }

    // 제목: 기존 규칙 그대로 (저축이고 사용처가 비어 있으면 카테고리)
    val title = if (transaction.type == TransactionType.SAVING && transaction.merchant.isBlank())
        transaction.category
    else
        transaction.merchant

    // 부가정보: 카테고리(제목과 같으면 생략) · 반복 주기 · 결제 수단
    val subText = listOfNotNull(
        transaction.category.takeIf { it != title },
        getPatternText(transaction, strings),
        getPaymentMethodDisplayName(transaction.paymentMethod, strings)
    ).joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isHighlighted) BrandColor.mint.copy(alpha = 0.06f) else Color.Transparent)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(start = 10.dp, end = 6.dp, top = 10.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 꺼진 항목은 흐리게 (스위치는 그대로)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (transaction.isActive) 1f else 0.45f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 카테고리 아이콘 타일 (거래 유형 색으로 은은하게)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(typeColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = categoryEmoji, fontSize = 19.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 금액
                Text(
                    text = when (transaction.type) {
                        TransactionType.INCOME -> "+${strings.amountWithUnit(Utils.formatAmount(transaction.amount))}"
                        TransactionType.EXPENSE -> strings.amountWithUnit(Utils.formatAmount(transaction.amount))
                        TransactionType.SAVING -> strings.amountWithUnit(Utils.formatAmount(transaction.amount))
                        TransactionType.INVESTMENT -> strings.amountWithUnit(Utils.formatAmount(transaction.amount))
                    },
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 활성화 토글
            Switch(
                checked = transaction.isActive,
                onCheckedChange = { onToggleActive() },
                colors = mintSwitchColors(),
                modifier = Modifier.scale(0.85f)
            )
        }

        // 하단: (오늘 항목) 탭 안내 + 수정 / 삭제
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isHighlighted) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.tapToAddTransaction,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandColor.mint
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BrandColor.mint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            ItemTextAction(
                icon = Icons.Default.Edit,
                label = strings.edit,
                onClick = onEdit
            )
            ItemTextAction(
                icon = Icons.Default.Delete,
                label = strings.delete,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ItemTextAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        RecurringPattern.DAILY -> {
            if (transaction.includeWeekends) {
                strings.everyDay
            } else {
                "${strings.everyDay} (${strings.excludeWeekendsOption})"
            }
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
