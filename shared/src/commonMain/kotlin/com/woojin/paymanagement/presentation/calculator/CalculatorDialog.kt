package com.woojin.paymanagement.presentation.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.woojin.paymanagement.theme.BrandColor
import kotlinx.datetime.DayOfWeek
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.usecase.CalculatorUseCase
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.theme.SavingColor
import com.woojin.paymanagement.utils.Utils
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorDialog(
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
    initialPayPeriod: com.woojin.paymanagement.utils.PayPeriod? = null,
    allCategories: List<com.woojin.paymanagement.data.Category> = emptyList()
) {
    val calculatorUseCase = remember { CalculatorUseCase() }
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // 급여기간이 있으면 그 기간을 사용, 없으면 오늘부터 1달 전
    val defaultStartDate = initialPayPeriod?.startDate ?: today.minus(1, DateTimeUnit.MONTH)
    val defaultEndDate = initialPayPeriod?.endDate ?: today

    var startDate by remember { mutableStateOf(defaultStartDate) }
    var endDate by remember { mutableStateOf(defaultEndDate) }
    var selectedTransactionType by remember { mutableStateOf<TransactionType?>(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var calculatorResult by remember { mutableStateOf<CalculatorResult?>(null) }

    // 날짜 범위 선택 다이얼로그 상태
    var showDateRangePicker by remember { mutableStateOf(false) }

    // 스크롤 상태
    val scrollState = rememberScrollState()

    // 사용 가능한 카테고리 목록 (거래 내역이 1건 이상 있는 카테고리만)
    val availableCategories = remember(transactions, startDate, endDate, selectedTransactionType) {
        calculatorUseCase.getAvailableCategories(
            transactions,
            startDate,
            endDate,
            selectedTransactionType
        )
    }

    // 사용 가능한 카테고리가 변경되면 첫 번째 카테고리를 자동 선택
    LaunchedEffect(availableCategories) {
        if (availableCategories.isNotEmpty() && selectedCategory == null) {
            selectedCategory = availableCategories.first()
        } else if (availableCategories.isNotEmpty() && selectedCategory !in availableCategories) {
            // 현재 선택된 카테고리가 목록에 없으면 첫 번째 카테고리 선택
            selectedCategory = availableCategories.first()
        }
    }

    // 자동 계산: 카테고리 선택이 변경될 때마다 자동으로 계산
    LaunchedEffect(selectedCategory, startDate, endDate, selectedTransactionType) {
        if (selectedCategory != null) {
            val request = CalculatorRequest(
                startDate = startDate,
                endDate = endDate,
                transactionType = selectedTransactionType,
                categories = listOf(selectedCategory!!)
            )
            calculatorResult = calculatorUseCase.calculate(transactions, request)
        }
    }

    val strings = LocalStrings.current
    val typeColor = when (selectedTransactionType) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        TransactionType.SAVING -> SavingColor.color
        else -> MaterialTheme.colorScheme.error
    }

    // 다이얼로그 구조는 유지하되, 화면 전체를 덮도록 표시
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 상단 바: 왼쪽 닫기(X) + 제목
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = strings.periodCalculator,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    // 기간 (누르면 기존 기간 수정 창)
                    SectionLabel(strings.periodSetting)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showDateRangePicker = true }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = BrandColor.mint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${startDate.year}.${startDate.monthNumber.toString().padStart(2, '0')}.${startDate.dayOfMonth.toString().padStart(2, '0')} ~ ${endDate.year}.${endDate.monthNumber.toString().padStart(2, '0')}.${endDate.dayOfMonth.toString().padStart(2, '0')}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = strings.editPeriod,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandColor.mint
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = BrandColor.mint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 거래 유형 (세그먼트)
                    SectionLabel(strings.transactionType)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        listOf(
                            Triple(TransactionType.INCOME, strings.income, MaterialTheme.colorScheme.primary),
                            Triple(TransactionType.EXPENSE, strings.expense, MaterialTheme.colorScheme.error),
                            Triple(TransactionType.SAVING, strings.saving, SavingColor.color)
                        ).forEach { (type, label, color) ->
                            val isSelected = selectedTransactionType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (isSelected) Modifier.shadow(1.dp, RoundedCornerShape(10.dp))
                                        else Modifier
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable {
                                        // 거래 타입 변경 시 카테고리는 LaunchedEffect에서 자동으로 설정됨
                                        selectedTransactionType = type
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 카테고리 (칩 유지, 한 개만 선택)
                    SectionLabel(strings.category)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (availableCategories.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableCategories.forEach { category ->
                                val isSelected = category == selectedCategory
                                val backgroundColor = when {
                                    isSelected && selectedTransactionType == TransactionType.INCOME -> Color(0xFFE3F2FD) // 연한 파랑
                                    isSelected && selectedTransactionType == TransactionType.EXPENSE -> Color(0xFFFFEBEE) // 연한 빨강
                                    isSelected && selectedTransactionType == TransactionType.SAVING -> SavingColor.lightBackground
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val borderColor = if (isSelected) typeColor else Color.Transparent
                                val shape = RoundedCornerShape(50)
                                Row(
                                    modifier = Modifier
                                        .clip(shape)
                                        .background(color = backgroundColor, shape = shape)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = borderColor,
                                            shape = shape
                                        )
                                        .clickable {
                                            // 이미 선택된 카테고리를 다시 클릭하면 선택 해제하지 않음
                                            if (selectedCategory != category) {
                                                selectedCategory = category
                                            }
                                        }
                                        .padding(horizontal = 13.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    val emoji = com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji(
                                        category,
                                        allCategories
                                    )
                                    if (emoji.isNotBlank()) {
                                        Text(text = emoji, fontSize = 14.sp)
                                    }
                                    Text(
                                        text = category,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = strings.noTransactions,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 계산 결과
                    calculatorResult?.let { result ->
                        val typeLabel = when (selectedTransactionType) {
                            TransactionType.INCOME -> strings.income
                            TransactionType.SAVING -> strings.saving
                            else -> strings.expense
                        }
                        val categoryTitle = selectedCategory?.let { category ->
                            val emoji = com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji(category, allCategories)
                            if (emoji.isNotBlank()) "$emoji $category" else category
                        } ?: ""
                        CalculatorResultCard(
                            result = result,
                            title = "$categoryTitle · $typeLabel",
                            periodText = "${startDate.monthNumber}.${startDate.dayOfMonth} ~ ${endDate.monthNumber}.${endDate.dayOfMonth}"
                        )

                        // 거래 상세 내역
                        if (result.transactionDetails.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = strings.transactionDetail,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            result.transactionDetails.forEachIndexed { index, detail ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                    )
                                }
                                TransactionDetailItem(
                                    detail = detail,
                                    transactionType = selectedTransactionType,
                                    amountColor = typeColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // 날짜 범위 선택 다이얼로그
        if (showDateRangePicker) {
            DateRangePickerDialog(
                initialStartDate = startDate,
                initialEndDate = endDate,
                maxDate = today,
                onDateRangeSelected = { newStartDate, newEndDate ->
                    startDate = newStartDate
                    endDate = newEndDate
                    showDateRangePicker = false
                },
                onDismiss = { showDateRangePicker = false }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** 계산 결과 카드: 민트 그라데이션 + 총액 강조 + 건수/평균 */
@Composable
private fun CalculatorResultCard(
    result: CalculatorResult,
    title: String,
    periodText: String
) {
    val strings = LocalStrings.current
    val onCard = Color.White
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(BrandColor.mint, Color(0xFF47B49C))))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = onCard,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(onCard.copy(alpha = 0.2f))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = periodText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onCard
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = strings.totalAmount,
            fontSize = 12.sp,
            color = onCard.copy(alpha = 0.85f)
        )
        Text(
            text = strings.amountWithUnit(Utils.formatAmount(result.totalAmount)),
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = onCard
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(onCard.copy(alpha = 0.16f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(text = strings.transactionCountLabel, fontSize = 12.sp, color = onCard.copy(alpha = 0.85f))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.transactionCount(result.transactionCount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = onCard
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(onCard.copy(alpha = 0.35f))
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = strings.averageAmount, fontSize = 12.sp, color = onCard.copy(alpha = 0.85f))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.amountWithUnit(Utils.formatAmount(result.averageAmount)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = onCard
                )
            }
        }
    }
}

/** 거래 상세 한 줄: 사용처(굵게) / 날짜(요일) · 메모(회색) / 금액 */
@Composable
private fun TransactionDetailItem(
    detail: TransactionDetail,
    transactionType: TransactionType?,
    amountColor: Color
) {
    val strings = LocalStrings.current
    val weekday = when (detail.date.dayOfWeek) {
        DayOfWeek.MONDAY -> strings.monday
        DayOfWeek.TUESDAY -> strings.tuesday
        DayOfWeek.WEDNESDAY -> strings.wednesday
        DayOfWeek.THURSDAY -> strings.thursday
        DayOfWeek.FRIDAY -> strings.friday
        DayOfWeek.SATURDAY -> strings.saturday
        else -> strings.sunday
    }
    val dateText = "${detail.date.monthNumber}.${detail.date.dayOfMonth} (${strings.weekdayShort(weekday)})"
    // 사용처가 없으면 메모를 제목으로 사용
    val hasMerchant = !detail.merchant.isNullOrBlank()
    val title = if (hasMerchant) detail.merchant!! else detail.memo
    val subText = listOf(dateText, if (hasMerchant) detail.memo else "")
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val sign = if (transactionType == TransactionType.INCOME) "+" else "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = subText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$sign${strings.amountWithUnit(Utils.formatAmount(detail.amount))}",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
private fun DateRangePickerDialog(
    initialStartDate: LocalDate,
    initialEndDate: LocalDate,
    maxDate: LocalDate? = null,
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayMonth by remember { mutableStateOf(initialStartDate.monthNumber) }
    var displayYear by remember { mutableStateOf(initialStartDate.year) }
    var tempStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var tempEndDate by remember { mutableStateOf<LocalDate?>(null) }
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    val strings = LocalStrings.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header with month/year navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        if (displayMonth > 1) {
                            displayMonth--
                        } else {
                            displayMonth = 12
                            displayYear--
                        }
                    }) {
                        Text(
                            "◀",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize
                        )
                    }

                    Text(
                        text = strings.monthYear(displayYear, displayMonth),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = {
                        if (displayMonth < 12) {
                            displayMonth++
                        } else {
                            displayMonth = 1
                            displayYear++
                        }
                    }) {
                        Text(
                            "▶",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Day of week headers
                val dayHeaders = strings.weekdaysShort
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Headers
                    items(dayHeaders) { day ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Calendar days
                    val firstDayOfMonth = LocalDate(displayYear, displayMonth, 1)
                    val daysInMonth = when (displayMonth) {
                        2 -> if (displayYear % 4 == 0 && (displayYear % 100 != 0 || displayYear % 400 == 0)) 29 else 28
                        4, 6, 9, 11 -> 30
                        else -> 31
                    }

                    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.ordinal + 1 // Sunday = 1
                    val startDayOfWeek = if (firstDayOfWeek == 7) 0 else firstDayOfWeek

                    // Empty cells before first day
                    items(startDayOfWeek) {
                        Spacer(modifier = Modifier.aspectRatio(1f))
                    }

                    // Days in month
                    items(daysInMonth) { dayIndex ->
                        val day = dayIndex + 1
                        val date = LocalDate(displayYear, displayMonth, day)

                        // 날짜 선택 상태 확인
                        val isStartDate = date == tempStartDate
                        val isEndDate = date == tempEndDate
                        val isInRange = tempStartDate != null && tempEndDate != null &&
                                       date >= tempStartDate!! && date <= tempEndDate!!
                        val isToday = date == today

                        // 날짜 선택 가능 여부 체크
                        val isDisabled = maxDate != null && date > maxDate

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clickable(enabled = !isDisabled) {
                                    if (!isDisabled) {
                                        when {
                                            // 첫 번째 클릭: 시작일 설정
                                            tempStartDate == null -> {
                                                tempStartDate = date
                                                tempEndDate = null
                                            }
                                            // 두 번째 클릭: 종료일 설정
                                            tempEndDate == null -> {
                                                if (date >= tempStartDate!!) {
                                                    tempEndDate = date
                                                } else {
                                                    // 시작일보다 이전 날짜를 선택하면 시작일을 새로 설정
                                                    tempStartDate = date
                                                    tempEndDate = null
                                                }
                                            }
                                            // 이미 범위가 선택된 경우: 다시 시작
                                            else -> {
                                                tempStartDate = date
                                                tempEndDate = null
                                            }
                                        }
                                    }
                                }
                                .background(
                                    when {
                                        isStartDate || isEndDate -> MaterialTheme.colorScheme.primary
                                        isInRange -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    },
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isDisabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    isStartDate || isEndDate -> Color.White
                                    isInRange -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isStartDate || isEndDate || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 선택 안내 텍스트
                Text(
                    text = when {
                        tempStartDate == null -> strings.selectStartDate
                        tempEndDate == null -> strings.selectEndDate
                        else -> "${strings.selectionComplete}: ${tempStartDate!!.year}.${tempStartDate!!.monthNumber.toString().padStart(2, '0')}.${tempStartDate!!.dayOfMonth.toString().padStart(2, '0')} ~ ${tempEndDate!!.year}.${tempEndDate!!.monthNumber.toString().padStart(2, '0')}.${tempEndDate!!.dayOfMonth.toString().padStart(2, '0')}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (tempStartDate != null && tempEndDate != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.cancel, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            if (tempStartDate != null && tempEndDate != null) {
                                onDateRangeSelected(tempStartDate!!, tempEndDate!!)
                            }
                        },
                        enabled = tempStartDate != null && tempEndDate != null,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(strings.confirm, color = Color.White)
                    }
                }
            }
        }
    }
}