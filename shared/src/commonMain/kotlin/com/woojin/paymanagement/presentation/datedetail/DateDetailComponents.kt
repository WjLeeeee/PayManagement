package com.woojin.paymanagement.presentation.datedetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.domain.model.DailySummary
import com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji
import com.woojin.paymanagement.presentation.addtransaction.formatCategoryDisplay
import com.woojin.paymanagement.strings.AppStrings
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.theme.BrandColor
import com.woojin.paymanagement.theme.InvestmentColor
import com.woojin.paymanagement.theme.SavingColor
import com.woojin.paymanagement.utils.Utils
import kotlinx.datetime.LocalDate

private const val ExpandAnimMs = 280

/**
 * 결제수단을 한글로 변환합니다.
 */
private fun getPaymentMethodText(paymentMethod: PaymentMethod?, strings: AppStrings, cardName: String? = null): String {
    return when (paymentMethod) {
        PaymentMethod.CASH -> strings.cash
        PaymentMethod.CARD -> cardName ?: strings.card
        PaymentMethod.BALANCE_CARD -> cardName ?: strings.balanceCard
        PaymentMethod.GIFT_CARD -> cardName ?: strings.giftCard
        null -> ""
    }
}

/**
 * 수입유형을 다국어로 변환합니다.
 */
private fun getIncomeTypeText(incomeType: IncomeType?, strings: AppStrings, cardName: String? = null): String {
    return when (incomeType) {
        IncomeType.CASH -> strings.cash
        IncomeType.BALANCE_CARD -> cardName ?: strings.balanceCard
        IncomeType.GIFT_CARD -> cardName ?: strings.giftCard
        null -> ""
    }
}

@Composable
private fun typeColorOf(type: TransactionType): Color = when (type) {
    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
    TransactionType.SAVING -> SavingColor.color
    TransactionType.INVESTMENT -> InvestmentColor.color
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
                // weekdaysShort는 일요일부터 시작, dayOfWeek.ordinal은 월요일=0
                val weekday = strings.weekdaysShort[(selectedDate.dayOfWeek.ordinal + 1) % 7]
                "${strings.fullDate(selectedDate.year, selectedDate.monthNumber, selectedDate.dayOfMonth)} ($weekday)"
            } else {
                strings.dateDetail
            },
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 일일 요약: "당일 지출"을 큰 숫자로, 수입·저축·투자는 칩으로 표시
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailySummaryCard(
    summary: DailySummary
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = strings.expenseForDay,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = strings.amountWithUnit(Utils.formatAmount(summary.totalExpense)),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 수입
            SummaryChip(
                text = "${strings.income} +${strings.amountWithUnit(Utils.formatAmount(summary.totalIncome))}",
                color = MaterialTheme.colorScheme.primary
            )

            // 저축 합계 (저축 거래가 있을 때만 표시)
            if (summary.totalSaving > 0) {
                SummaryChip(
                    text = "🐷 ${strings.saving} -${strings.amountWithUnit(Utils.formatAmount(summary.totalSaving))}",
                    color = SavingColor.color
                )
            }

            // 투자 합계 (투자 거래가 있을 때만 표시)
            if (summary.totalInvestment != 0.0) {
                SummaryChip(
                    text = "💹 ${strings.investment} ${if (summary.totalInvestment > 0) "+" else "-"}${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(summary.totalInvestment)))}",
                    color = InvestmentColor.color
                )
            }
        }
    }
}

@Composable
private fun SummaryChip(
    text: String,
    color: Color,
    background: Color = color.copy(alpha = 0.1f)
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 화면 좌우 끝까지 이어지는 회색 구분 띠 (메인 화면과 동일)
 */
@Composable
fun DateDetailSectionBand(horizontalBleed: Dp = 16.dp) {
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
fun TransactionListHeader(
    transactionCount: Int
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings.transactionsForDay,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = strings.transactionCount(transactionCount),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 하단 고정 "거래 추가" 버튼
 */
@Composable
fun AddTransactionBottomButton(
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandColor.mint,
            contentColor = Color.White
        )
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = strings.addTransaction,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.ExtraBold
        )
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
    availableCategories: List<com.woojin.paymanagement.data.Category> = emptyList(),
    showMineIndicator: Boolean = false
) {
    val strings = LocalStrings.current
    val typeColor = typeColorOf(transaction.type)

    val rowBackground by animateColorAsState(
        targetValue = if (isExpanded) BrandColor.mint.copy(alpha = 0.06f) else Color.Transparent,
        animationSpec = tween(ExpandAnimMs)
    )

    // 결제수단/수입유형 (기존 규칙 그대로)
    val methodText = when (transaction.type) {
        TransactionType.EXPENSE -> getPaymentMethodText(transaction.paymentMethod, strings, transaction.cardName)
        TransactionType.INCOME -> getIncomeTypeText(transaction.incomeType, strings, transaction.cardName)
        TransactionType.SAVING -> ""
        TransactionType.INVESTMENT -> ""
    }
    // 두 번째 줄: 사용처 · 결제수단
    val subText = listOf(transaction.merchant.orEmpty().trim(), methodText.trim())
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        // 기본 정보 (항상 표시)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 카테고리 아이콘 타일 (거래 유형 색으로 은은하게)
            val categoryEmoji = getCategoryEmoji(transaction.category, availableCategories)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(typeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (categoryEmoji.isNotBlank()) {
                    Text(text = categoryEmoji, fontSize = 19.sp)
                } else {
                    Text(
                        text = transaction.category.take(1),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatCategoryDisplay(transaction.category, transaction.subCategory),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (showMineIndicator) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "나",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                if (subText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 금액
            val investmentIncomeCategories = setOf("익절", "배당금")
            Text(
                text = "${when (transaction.type) {
                    TransactionType.INCOME -> "+"
                    TransactionType.EXPENSE -> "-"
                    TransactionType.SAVING -> "-"
                    TransactionType.INVESTMENT -> if (transaction.category in investmentIncomeCategories) "+" else "-"
                }}${
                    strings.amountWithUnit(Utils.formatAmount(transaction.displayAmount))
                }",
                fontSize = 14.5.sp,
                color = typeColor,
                fontWeight = FontWeight.Bold
            )
        }

        // 확장 영역: 메모 + 버튼들
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(tween(ExpandAnimMs)) + fadeIn(tween(ExpandAnimMs)),
            exit = shrinkVertically(tween(ExpandAnimMs)) + fadeOut(tween(ExpandAnimMs))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 메모 (있는 경우만 표시)
                if (transaction.memo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = strings.memo,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.memo,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 반복/편집/삭제 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActionPill(
                        icon = Icons.Default.Refresh,
                        label = strings.recurringShort,
                        contentDescription = strings.saveAsRecurring,
                        color = BrandColor.mint,
                        background = BrandColor.mint.copy(alpha = 0.1f),
                        onClick = onSaveAsRecurring,
                        modifier = Modifier.weight(1f)
                    )
                    ActionPill(
                        icon = Icons.Default.Edit,
                        label = strings.edit,
                        contentDescription = strings.edit,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        background = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    )
                    ActionPill(
                        icon = Icons.Default.Delete,
                        label = strings.delete,
                        contentDescription = strings.delete,
                        color = MaterialTheme.colorScheme.error,
                        background = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    color: Color,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EmptyTransactionMessage() {
    val strings = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📭", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.noTransactionsOnDate,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
