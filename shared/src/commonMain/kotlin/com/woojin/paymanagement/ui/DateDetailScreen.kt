package com.woojin.paymanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.utils.Utils
import kotlinx.datetime.LocalDate

@Composable
fun DateDetailScreen(
    selectedDate: LocalDate?,
    transactions: List<Transaction>,
    onBack: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onAddTransaction: () -> Unit
) {
    val dayTransactions = selectedDate?.let { date ->
        transactions.filter { it.date == date }
    } ?: emptyList()

    val totalIncome =
        dayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense =
        dayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val dailyBalance = totalIncome - totalExpense

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
                text = if (selectedDate != null) {
                    "${selectedDate.year}년 ${selectedDate.monthNumber}월 ${selectedDate.dayOfMonth}일"
                } else {
                    "날짜 상세"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.LightGray
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "일일 요약",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("수입", color = Color.Blue)
                        Text(
                            text = "+${Utils.formatAmount(totalIncome)}원",
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                    }

                    Column {
                        Text("지출", color = Color.Red)
                        Text(
                            text = "-${Utils.formatAmount(totalExpense)}원",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }

                    Column {
                        Text("총")
                        Text(
                            text = "${if (dailyBalance >= 0) "+" else ""}${Utils.formatAmount(dailyBalance)}원",
                            fontWeight = FontWeight.Bold,
                            color = if (dailyBalance >= 0) Color.Blue else Color.Red
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transactions list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "거래 내역 (${dayTransactions.size}건)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onAddTransaction,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) {
                Text("추가", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (dayTransactions.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(dayTransactions) { transaction ->
                    TransactionDetailItem(
                        transaction = transaction,
                        onEdit = { onEditTransaction(transaction) },
                        onDelete = { onDeleteTransaction(transaction) }
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.LightGray
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "이 날짜에 거래 내역이 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TransactionDetailItem(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f)
                ) {
                    // 거래 유형 표시
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .offset(y = 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (transaction.type == TransactionType.INCOME) Color.Blue else Color.Red
                            )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )

                        Text(
                            text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"}${Utils.formatAmount(transaction.amount)}원",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (transaction.type == TransactionType.INCOME) Color.Blue else Color.Red,
                            fontWeight = FontWeight.Bold
                        )

                        if (transaction.memo.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = transaction.memo,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Action buttons
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "편집",
                            tint = Color.Blue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}