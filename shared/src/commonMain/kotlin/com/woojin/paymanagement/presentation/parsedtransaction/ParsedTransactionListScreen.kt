package com.woojin.paymanagement.presentation.parsedtransaction

import androidx.compose.foundation.clickable
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
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(hasNotificationPermission) }
    val strings = LocalStrings.current

    // 시스템 뒤로가기 버튼 처리 (Android에서만 동작, iOS에서는 자동으로 무시됨)
    BackHandler(onBack = onBack)

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
            .padding(16.dp)
    ) {
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
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            // 알림 버튼 - 클릭 시 바로 설정 화면으로 이동
            TextButton(
                onClick = {
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
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (hasPermission) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = if (hasPermission) strings.notificationOn else strings.notificationOff,
                    fontWeight = if (hasPermission) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = strings.parsedTransactionDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                        Text(
                            text = strings.noParsedTransactions,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.cardNotificationAutoDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.parsedTransactions) { transaction ->
                        ParsedTransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction) },
                            onDelete = {
                                scope.launch {
                                    viewModel.deleteParsedTransaction(transaction.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsedTransactionItem(
    transaction: ParsedTransaction,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 가맹점명
                Text(
                    text = transaction.merchantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 금액
                Text(
                    text = strings.amountWithUnit(transaction.amount.toInt().toString()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 날짜
                Text(
                    text = strings.shortDate(transaction.date.monthNumber, transaction.date.dayOfMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            TextButton(onClick = onDelete) {
                Text(strings.delete, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}