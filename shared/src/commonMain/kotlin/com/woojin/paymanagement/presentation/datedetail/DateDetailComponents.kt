package com.woojin.paymanagement.presentation.datedetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.domain.model.DailySummary
import com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji
import com.woojin.paymanagement.strings.AppStrings
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.Utils
import kotlinx.datetime.LocalDate

/**
 * 결제수단을 한글로 변환합니다.
 */
private fun getPaymentMethodText(paymentMethod: PaymentMethod?, strings: AppStrings): String {
    return when (paymentMethod) {
        PaymentMethod.CASH -> strings.cash
        PaymentMethod.CARD -> strings.card
        PaymentMethod.BALANCE_CARD -> strings.balanceCard
        PaymentMethod.GIFT_CARD -> strings.giftCard
        null -> ""
    }
}

/**
 * 수입유형을 다국어로 변환합니다.
 */
private fun getIncomeTypeText(incomeType: IncomeType?, strings: AppStrings): String {
    return when (incomeType) {
        IncomeType.CASH -> strings.cash
        IncomeType.BALANCE_CARD -> strings.balanceCard
        IncomeType.GIFT_CARD -> strings.giftCard
        null -> ""
    }
}

@Composable
fun DateDetailHeader(
    selectedDate: LocalDate?,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
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
            text = if (selectedDate != null) {
                "📅 ${strings.fullDate(selectedDate.year, selectedDate.monthNumber, selectedDate.dayOfMonth)}"
            } else {
                "📅 ${strings.dateDetail}"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DailySummaryCard(
    summary: DailySummary
) {
    val strings = LocalStrings.current
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
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 ${strings.dailySummary}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
                            text = strings.income,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "+${strings.amountWithUnit(Utils.formatAmount(summary.totalIncome))}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
                            text = strings.expense,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "-${strings.amountWithUnit(Utils.formatAmount(summary.totalExpense))}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
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
                            text = strings.balance,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${
                            when {
                                summary.dailyBalance > 0 -> "+"
                                summary.dailyBalance < 0 -> "-"
                                else -> ""
                            }
                        }${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(summary.dailyBalance)))}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            summary.dailyBalance > 0 -> MaterialTheme.colorScheme.primary
                            summary.dailyBalance < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
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
        val strings = LocalStrings.current
        Text(
            text = "📝 ${strings.transactionListHeader(transactionCount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Button(
            onClick = onAddTransaction,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("➕ ${strings.add}", color = Color.White)
        }
    }
}

@Composable
fun TransactionDetailItem(
    transaction: Transaction,
    isExpanded: Boolean = false,
    onClick: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSaveAsRecurring: () -> Unit,
    availableCategories: List<com.woojin.paymanagement.data.Category> = emptyList()
) {
    val strings = LocalStrings.current
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            // 기본 정보 (항상 표시): 카테고리, 금액, 사용처
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 거래 유형 표시
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(y = 8.dp)
                        .clip(CircleShape)
                        .background(
                            when (transaction.type) {
                                TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                                TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                                TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                            }
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // 카테고리와 결제수단
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = getCategoryEmoji(transaction.category, availableCategories),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 결제수단/수입유형 표시
                        val methodText = when (transaction.type) {
                            TransactionType.EXPENSE -> getPaymentMethodText(transaction.paymentMethod, strings)
                            TransactionType.INCOME -> getIncomeTypeText(transaction.incomeType, strings)
                            TransactionType.SAVING -> ""
                        }

                        if (methodText.isNotBlank()) {
                            Text(
                                text = "($methodText)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 금액
                    Text(
                        text = "${when (transaction.type) {
                            TransactionType.INCOME -> "+"
                            TransactionType.EXPENSE -> "-"
                            TransactionType.SAVING -> "-"
                        }}${
                            strings.amountWithUnit(Utils.formatAmount(transaction.displayAmount))
                        }",
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (transaction.type) {
                            TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                            TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                            TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                        },
                        fontWeight = FontWeight.Bold
                    )

                    // 사용처 (있는 경우만 표시)
                    if (!transaction.merchant.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📍 ${transaction.merchant}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 확장 영역: 메모 + 버튼들
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                        .padding(16.dp)
                ) {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )

                    // 메모 (있는 경우만 표시)
                    if (transaction.memo.isNotBlank()) {
                        Text(
                            text = strings.memo,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = transaction.memo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // 반복/편집/삭제 버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onSaveAsRecurring,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = strings.saveAsRecurring,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.recurringShort, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = strings.edit,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.edit, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = strings.delete,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.delete, style = MaterialTheme.typography.bodySmall)
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            val strings = LocalStrings.current
            Text(
                text = "📭 ${strings.noTransactionsOnDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}