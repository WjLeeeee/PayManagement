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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.woojin.paymanagement.utils.BackHandler
import kotlinx.coroutines.launch

@Composable
fun ParsedTransactionListScreen(
    viewModel: ParsedTransactionViewModel,
    onTransactionClick: (ParsedTransaction) -> Unit,
    onBack: () -> Unit,
    onSendTestNotifications: ((List<ParsedTransaction>) -> Unit)? = null,
    hasNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onCheckPermission: () -> Boolean = { true }
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }
    var isNotificationEnabled by remember { mutableStateOf(true) }
    var hasPermission by remember { mutableStateOf(hasNotificationPermission) }

    // 시스템 뒤로가기 버튼 처리 (Android에서만 동작, iOS에서는 자동으로 무시됨)
    BackHandler(onBack = onBack)

    // 화면이 보일 때마다 권한 상태 체크 (DisposableEffect 사용)
    DisposableEffect(Unit) {
        hasPermission = onCheckPermission()
        onDispose { }
    }

    // 주기적으로 권한 상태 체크 (1초마다)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            hasPermission = onCheckPermission()
        }
    }

    // 권한 안내 다이얼로그
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("알림 전송 권한 필요") },
            text = { Text("푸시 알림 기능을 사용하려면 알림 전송 권한이 필요합니다.\n\n설정 화면으로 이동하시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    onRequestNotificationPermission()
                }) {
                    Text("설정하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 알림 비활성화 확인 다이얼로그
    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            title = { Text("알림 끄기") },
            text = { Text("푸시 알림 기능을 끄시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    isNotificationEnabled = false
                    showDisableDialog = false
                }) {
                    Text("끄기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }) {
                    Text("취소")
                }
            }
        )
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }

            Text(
                text = "카드 결제 내역",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // 알림 버튼
            val isEnabled = hasPermission && isNotificationEnabled
            TextButton(
                onClick = {
                    if (!hasPermission) {
                        // 권한이 없으면 권한 요청 다이얼로그
                        showPermissionDialog = true
                    } else if (isNotificationEnabled) {
                        // 알림이 켜져 있으면 끄기 다이얼로그
                        showDisableDialog = true
                    } else {
                        // 알림이 꺼져 있으면 다시 켜기
                        isNotificationEnabled = true
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isEnabled) Color(0xFF4CAF50) else Color.Gray
                )
            ) {
                Text(
                    text = if (isEnabled) "🔔 알림" else "🔕 알림",
                    fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal
                )
            }

            // 테스트 데이터 추가 버튼
            TextButton(
                onClick = {
                    scope.launch {
                        val testTransactions = viewModel.addTestData()
                        // 권한과 알림 활성화 상태 모두 확인
                        if (hasPermission && isNotificationEnabled) {
                            onSendTestNotifications?.invoke(testTransactions)
                        }
                    }
                }
            ) {
                Text("테스트", color = Color.Blue)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "알림에서 파싱된 거래 내역입니다. 항목을 클릭하여 거래를 추가하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
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
                        text = "오류: ${uiState.error}",
                        color = Color.Red
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
                            text = "파싱된 거래 내역이 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "카드 결제 알림이 오면 자동으로 표시됩니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
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
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 금액
                Text(
                    text = "${transaction.amount.toInt()}원",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Red,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 날짜
                Text(
                    text = "${transaction.date.monthNumber}월 ${transaction.date.dayOfMonth}일",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Delete button
            TextButton(onClick = onDelete) {
                Text("삭제", color = Color.Red)
            }
        }
    }
}