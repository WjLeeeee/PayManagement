package com.woojin.paymanagement.presentation.paydaysetup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.strings.AppStrings
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.theme.BrandColor
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.PaydayAdjustment
import kotlinx.datetime.LocalDate

private val PaydayGold = Color(0xFFF0B04C)

private fun AppStrings.weekdayOf(date: LocalDate): String =
    weekdaysShort[(date.dayOfWeek.ordinal + 1) % 7]

/**
 * 상단 결과 배너: "내 월급날 / 매달 25일" + 이번 급여 기간·다음 월급날 칩. 고르는 즉시 바뀜
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaydayHeroBanner(
    payday: Int,
    currentPeriod: PayPeriod?,
    nextPayday: LocalDate?,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(BrandColor.mint.copy(alpha = 0.12f), PaydayGold.copy(alpha = 0.12f))
                )
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = strings.myPayday,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = buildAnnotatedString {
                append(strings.paydayHeroPrefix)
                withStyle(SpanStyle(color = BrandColor.mint)) { append(strings.paydayHeroDay(payday)) }
            },
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (currentPeriod != null && nextPayday != null) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaydayInfoChip(
                    text = strings.currentPeriodChip(
                        currentPeriod.startDate.monthNumber, currentPeriod.startDate.dayOfMonth,
                        currentPeriod.endDate.monthNumber, currentPeriod.endDate.dayOfMonth
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PaydayInfoChip(
                    text = strings.nextPaydayChip(
                        nextPayday.monthNumber, nextPayday.dayOfMonth, strings.weekdayOf(nextPayday)
                    ),
                    color = BrandColor.mint
                )
            }
        }
    }
}

@Composable
private fun PaydayInfoChip(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    )
}

@Composable
fun PaydaySectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

/**
 * 1~31일 원형 그리드 (7열, 31일까지 모두 보임)
 */
@Composable
fun PaydayGrid(
    selectedPayday: Int,
    onPaydaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        (1..31).chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                week.forEach { day ->
                    val isSelected = day == selectedPayday
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) BrandColor.mint
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onPaydaySelected(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * 주말·공휴일 조정 세그먼트 [이전 평일 | 다음 평일]
 */
@Composable
fun PaydayAdjustmentSegment(
    selectedAdjustment: PaydayAdjustment,
    onAdjustmentSelected: (PaydayAdjustment) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        listOf(
            PaydayAdjustment.BEFORE_WEEKEND to strings.beforeWeekdayShort,
            PaydayAdjustment.AFTER_WEEKEND to strings.afterWeekdayShort
        ).forEach { (adjustment, label) ->
            val isSelected = adjustment == selectedAdjustment
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(10.dp)) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onAdjustmentSelected(adjustment) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.5.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isSelected) BrandColor.mint else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * "10월 25일(일) → 10월 23일(금)에 받아요" (옮겨지지 않으면 "다음 월급은 ○월 ○일(○)에 받아요")
 */
@Composable
fun PaydayShiftNote(
    nextPayday: LocalDate?,
    nextPaydayNominal: LocalDate?,
    modifier: Modifier = Modifier
) {
    if (nextPayday == null) return
    val strings = LocalStrings.current
    val from = nextPaydayNominal
        ?.takeIf { it != nextPayday }
        ?.let { strings.dateWithWeekday(it.monthNumber, it.dayOfMonth, strings.weekdayOf(it)) }
    Text(
        text = buildAnnotatedString {
            append(strings.paidOnPrefix(from))
            withStyle(SpanStyle(color = BrandColor.mint, fontWeight = FontWeight.Bold)) {
                append(strings.dateWithWeekday(nextPayday.monthNumber, nextPayday.dayOfMonth, strings.weekdayOf(nextPayday)))
            }
            append(strings.paidOnSuffix)
        },
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun PaydaySetupButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandColor.mint),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else {
            val strings = LocalStrings.current
            Text(
                text = strings.setupComplete,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun ErrorMessage(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            val strings = LocalStrings.current
            TextButton(onClick = onDismiss) {
                Text(strings.close, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
