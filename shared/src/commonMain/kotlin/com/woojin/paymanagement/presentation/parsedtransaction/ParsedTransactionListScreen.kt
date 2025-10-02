package com.woojin.paymanagement.presentation.parsedtransaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.ParsedTransaction
import kotlinx.coroutines.launch

@Composable
fun ParsedTransactionListScreen(
    viewModel: ParsedTransactionViewModel,
    onTransactionClick: (ParsedTransaction) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

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

            // 테스트 데이터 추가 버튼
            TextButton(
                onClick = {
                    scope.launch {
                        viewModel.addTestData()
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