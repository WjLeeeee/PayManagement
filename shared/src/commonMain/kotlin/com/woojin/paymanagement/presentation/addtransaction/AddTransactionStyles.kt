package com.woojin.paymanagement.presentation.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.theme.BrandColor
import com.woojin.paymanagement.theme.InvestmentColor
import com.woojin.paymanagement.theme.SavingColor

/** 거래 유형별 대표 색상 (수입 파랑 / 지출 빨강 / 저축 초록 / 투자 보라) */
@Composable
internal fun transactionTypeColor(type: TransactionType): Color = when (type) {
    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
    TransactionType.SAVING -> SavingColor.color
    TransactionType.INVESTMENT -> InvestmentColor.color
}

/** 섹션 상단의 작은 회색 라벨 */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** 선택형 알약(pill) 칩 - 선택 시 브랜드 민트 강조 */
@Composable
internal fun SelectablePillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) BrandColor.mint.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) BrandColor.mint else Color.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (!emoji.isNullOrBlank()) {
            Text(text = emoji, fontSize = 14.sp)
        }
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) BrandColor.mint else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal val FilledInputShape = RoundedCornerShape(12.dp)
