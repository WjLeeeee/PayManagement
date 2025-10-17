package com.woojin.paymanagement.presentation.datedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.model.DailySummary
import com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji
import com.woojin.paymanagement.utils.Utils
import kotlinx.datetime.LocalDate

@Composable
fun DateDetailHeader(
    selectedDate: LocalDate?,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
        }

        Text(
            text = if (selectedDate != null) {
                "📅 ${selectedDate.year}년 ${selectedDate.monthNumber}월 ${selectedDate.dayOfMonth}일"
            } else {
                "📅 날짜 상세"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DailySummaryCard(
    summary: DailySummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
                            Color(0xFFF8FBFF), // 매우 연한 파랑
                            Color(0xFFFFFEF7), // 매우 연한 노랑
                            Color(0xFFFFFAFA)  // 매우 연한 빨강
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📊 일일 요약",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 수입
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💰",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "수입",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Blue
                            )
                        }
                        Text(
                            text = "+${Utils.formatAmount(summary.totalIncome)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                    }

                    // 지출
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💸",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "지출",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                        }
                        Text(
                            text = "-${Utils.formatAmount(summary.totalExpense)}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }

                    // 잔액
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💵",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "잔액",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = "${
                                when {
                                    summary.dailyBalance > 0 -> "+"
                                    summary.dailyBalance < 0 -> "-"
                                    else -> ""
                                }
                            }${Utils.formatAmount(kotlin.math.abs(summary.dailyBalance))}원",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                summary.dailyBalance > 0 -> Color.Blue
                                summary.dailyBalance < 0 -> Color.Red
                                else -> Color.Black
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionListHeader(
    transactionCount: Int,
    onAddTransaction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📝 거래 내역 (${transactionCount}건)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Button(
            onClick = onAddTransaction,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("➕ 추가", color = Color.White)
        }
    }
}

@Composable
fun TransactionDetailItem(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
                            Color(0xFFF8FBFF), // 매우 연한 파랑
                            Color(0xFFFFFEF7), // 매우 연한 노랑
                            Color(0xFFFFFAFA)  // 매우 연한 빨강
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
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.weight(1f)
                    ) {
                        // 거래 유형 표시
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .offset(y = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (transaction.type == TransactionType.INCOME) Color.Blue else Color.Red
                                )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = getCategoryEmoji(transaction.category),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = transaction.category,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

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
}

@Composable
fun EmptyTransactionMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
                            Color(0xFFF8FBFF), // 매우 연한 파랑
                            Color(0xFFFFFEF7), // 매우 연한 노랑
                            Color(0xFFFFFAFA)  // 매우 연한 빨강
                        )
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📭 이 날짜에 거래 내역이 없습니다",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
    }
}