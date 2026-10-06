package com.woojin.paymanagement.presentation.parsedtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.theme.BrandColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.ParsedTransaction
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.BackHandler
import com.woojin.paymanagement.utils.LifecycleObserverHelper
import kotlinx.coroutines.launch

@Composable
fun ParsedTransactionListScreen(
    viewModel: ParsedTransactionViewModel,
    onTransactionClick: (ParsedTransaction) -> Unit,
    onBack: () -> Unit,
    hasNotificationPermission: Boolean = true,
    onRequestPostNotificationPermission: ((onPermissionResult: (Boolean) -> Unit) -> Unit)? = null,
    onOpenNotificationSettings: () -> Unit = {},
    onCheckPermission: () -> Boolean = { true }
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(ParsedTab.PENDING) }
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(hasNotificationPermission) }
    var transactionToDelete by remember { mutableStateOf<ParsedTransaction?>(null) }
    val strings = LocalStrings.current

    // 시스템 뒤로가기 버튼 처리 (Android에서만 동작, iOS에서는 자동으로 무시됨)
    BackHandler(onBack = onBack)

    // 삭제 확인 다이얼로그
    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text(strings.delete) },
            text = { Text("${transaction.merchantName} (${strings.amountWithUnit(transaction.amount.toInt().toString())})\n${strings.deleteTransactionConfirm}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { viewModel.deleteParsedTransaction(transaction.id) }
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

    // 앱이 다시 포커스를 받았을 때 권한 상태 갱신 (설정에서 돌아올 때)
    val lifecycleObserver = remember { LifecycleObserverHelper() }
    lifecycleObserver.ObserveLifecycle {
        hasPermission = onCheckPermission()
    }

    // 화면이 보일 때마다 권한 상태 체크 (DisposableEffect 사용)
    DisposableEffect(Unit) {
        hasPermission = onCheckPermission()
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.goBack,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = strings.cardPaymentHistory,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            // 알림 버튼 - 클릭 시 바로 설정 화면으로 이동 (켜짐 민트 / 꺼짐 회색 알약)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (hasPermission) BrandColor.mint.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(onClick = {
                    if (!hasPermission) {
                        // 권한이 없으면 권한 요청 시도
                        onRequestPostNotificationPermission?.invoke { isGranted ->
                            hasPermission = isGranted
                            // 권한이 거부되었으면 설정 화면으로 이동
                            if (!isGranted) {
                                onOpenNotificationSettings()
                            }
                        }
                    } else {
                        // 권한이 있으면 바로 설정 화면으로 이동 (알림 끄기)
                        onOpenNotificationSettings()
                    }
                })
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (hasPermission) strings.notificationOn else strings.notificationOff,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasPermission) BrandColor.mint else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 기록 대기 / 기록 완료 탭
        ParsedTabBar(
            selectedTab = selectedTab,
            pendingCount = uiState.parsedTransactions.size,
            completedCount = uiState.processedTransactions.size,
            onTabSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == ParsedTab.PENDING) {
            // 안내 (민트 연한 상자)
            Text(
                text = "💡 ${strings.parsedTransactionDesc}",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandColor.mint,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandColor.mint.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.errorWithMessage(uiState.error ?: ""),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                uiState.parsedTransactions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(BrandColor.mint.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "💳", fontSize = 26.sp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = strings.noParsedTransactions,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.cardNotificationAutoDisplay,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.parsedTransactions) { transaction ->
                            ParsedTransactionItem(
                                transaction = transaction,
                                onClick = { onTransactionClick(transaction) },
                                onDelete = { transactionToDelete = transaction }
                            )
                        }
                    }
                }
            }
        } else {
            CompletedTransactionsContent(transactions = uiState.processedTransactions)
        }
    }
}

@Composable
private fun ParsedTransactionItem(
    transaction: ParsedTransaction,
    isHighlighted: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    val accent = BrandColor.mint

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isHighlighted) 1.5.dp else 1.dp,
                color = if (isHighlighted) BrandColor.mint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 카드 아이콘 타일
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💳", fontSize = 19.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 가맹점명
                Text(
                    text = transaction.merchantName,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                // 날짜
                Text(
                    text = strings.shortDate(transaction.date.monthNumber, transaction.date.dayOfMonth),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 금액
            Text(
                text = strings.amountWithUnit(transaction.amount.toInt().toString()),
                fontSize = 15.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // [삭제] [가계부에 추가]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.delete,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .weight(2f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(BrandColor.mint)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.addToLedger,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

private enum class ParsedTab { PENDING, COMPLETED }

/**
 * 기록 대기 / 기록 완료 세그먼트 탭 (선택 글자 민트 + 개수)
 */
@Composable
private fun ParsedTabBar(
    selectedTab: ParsedTab,
    pendingCount: Int,
    completedCount: Int,
    onTabSelected: (ParsedTab) -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        listOf(
            ParsedTab.PENDING to "${strings.pendingRecordTab} $pendingCount",
            ParsedTab.COMPLETED to "${strings.completedRecordTab} $completedCount"
        ).forEach { (tab, label) ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isSelected) BrandColor.mint else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 기록 완료 탭: 3일 보관 안내 + 완료 항목 목록 (보기 전용)
 */
@Composable
private fun ColumnScope.CompletedTransactionsContent(
    transactions: List<ParsedTransaction>
) {
    val strings = LocalStrings.current
    Text(
        text = strings.completedRetentionHint,
        fontSize = 12.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
    Spacer(modifier = Modifier.height(10.dp))

    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = strings.noCompletedRecords,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(transactions) { transaction ->
                CompletedTransactionItem(transaction = transaction)
            }
        }
    }
}

@Composable
private fun CompletedTransactionItem(transaction: ParsedTransaction) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 흐리게 표시 (이미 기록됨)
        Row(
            modifier = Modifier
                .weight(1f)
                .alpha(0.55f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💳", fontSize = 19.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchantName,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = strings.shortDate(transaction.date.monthNumber, transaction.date.dayOfMonth),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.amountWithUnit(transaction.amount.toInt().toString()),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        // ✓ 기록됨
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = BrandColor.mint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = strings.recordedBadge,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandColor.mint
            )
        }
    }
}
