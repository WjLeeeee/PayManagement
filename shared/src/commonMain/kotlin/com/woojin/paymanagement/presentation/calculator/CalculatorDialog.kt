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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.usecase.CalculatorUseCase
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
    initialPayPeriod: com.woojin.paymanagement.utils.PayPeriod? = null
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

    // 날짜 선택 다이얼로그 상태
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header
                Text(
                    text = "기간별 계산기",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // 기간 설정
                    Text(
                        text = "기간 설정",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showStartDatePicker = true }
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                "시작일",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                "${startDate.year}.${startDate.monthNumber}.${startDate.dayOfMonth}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                        }

                        Text(
                            "~",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = Color.Black
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEndDatePicker = true }
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                "종료일",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                "${endDate.year}.${endDate.monthNumber}.${endDate.dayOfMonth}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 거래 타입 선택
                    Text(
                        text = "거래 타입",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = {
                                selectedTransactionType = TransactionType.INCOME
                                // 거래 타입 변경 시 카테고리는 LaunchedEffect에서 자동으로 설정됨
                            },
                            label = {
                                Text(
                                    "수입",
                                    color = if (selectedTransactionType == TransactionType.INCOME) Color.White else Color.Black
                                )
                            },
                            selected = selectedTransactionType == TransactionType.INCOME,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.Blue,
                                selectedLabelColor = Color.White
                            )
                        )

                        FilterChip(
                            onClick = {
                                selectedTransactionType = TransactionType.EXPENSE
                                // 거래 타입 변경 시 카테고리는 LaunchedEffect에서 자동으로 설정됨
                            },
                            label = {
                                Text(
                                    "지출",
                                    color = if (selectedTransactionType == TransactionType.EXPENSE) Color.White else Color.Black
                                )
                            },
                            selected = selectedTransactionType == TransactionType.EXPENSE,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.Red,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 카테고리 선택
                    Text(
                        text = "카테고리",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 카테고리 목록
                    if (availableCategories.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableCategories.forEach { category ->
                                val isSelected = category == selectedCategory
                                val backgroundColor = when {
                                    isSelected && selectedTransactionType == TransactionType.INCOME -> Color(
                                        0xFFE3F2FD
                                    ) // 연한 파랑
                                    isSelected && selectedTransactionType == TransactionType.EXPENSE -> Color(
                                        0xFFFFEBEE
                                    ) // 연한 빨강
                                    else -> Color(0xFFF5F5F5) // 연한 회색
                                }
                                val borderColor = when {
                                    isSelected && selectedTransactionType == TransactionType.INCOME -> Color(
                                        0xFF2196F3
                                    ) // 파랑
                                    isSelected && selectedTransactionType == TransactionType.EXPENSE -> Color(
                                        0xFFF44336
                                    ) // 빨강
                                    else -> Color.Transparent
                                }
                                val textColor = when {
                                    isSelected -> Color.Black
                                    else -> Color.DarkGray
                                }

                                Row(
                                    modifier = Modifier
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .background(
                                            color = backgroundColor,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            // 이미 선택된 카테고리를 다시 클릭하면 선택 해제하지 않음
                                            if (selectedCategory != category) {
                                                selectedCategory = category
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji(
                                            category
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "거래 내역이 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 계산 결과
                    calculatorResult?.let { result ->
                        CalculatorResultCard(result = result)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 닫기 버튼
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text("닫기", color = Color.White)
                }
            }
        }

        // 날짜 선택 다이얼로그들
        if (showStartDatePicker) {
            CalendarDatePickerDialog(
                currentDate = startDate,
                maxDate = today, // 오늘 이후 날짜 선택 불가
                minDate = null,
                onDateSelected = { newDate ->
                    startDate = newDate
                    // 시작일이 종료일보다 이후면 종료일을 시작일로 맞춤
                    if (newDate > endDate) {
                        endDate = newDate
                    }
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false }
            )
        }

        if (showEndDatePicker) {
            CalendarDatePickerDialog(
                currentDate = endDate,
                maxDate = today, // 오늘 이후 날짜 선택 불가
                minDate = startDate, // 시작일 이전 날짜 선택 불가
                onDateSelected = { newDate ->
                    endDate = newDate
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false }
            )
        }
    }
}

@Composable
private fun CalculatorResultCard(result: CalculatorResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    text = "📊 계산 결과",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 요약 정보
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultSummaryItem(
                        label = "총액",
                        value = "${Utils.formatAmount(result.totalAmount)}원",
                        color = Color.Blue
                    )

                    ResultSummaryItem(
                        label = "거래 건수",
                        value = "${result.transactionCount}건",
                        color = Color.Black
                    )

                    ResultSummaryItem(
                        label = "평균 금액",
                        value = "${Utils.formatAmount(result.averageAmount)}원",
                        color = Color.Gray
                    )
                }

                // 거래 상세 내역
                if (result.transactionDetails.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "거래 상세",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    result.transactionDetails.forEach { detail ->
                        TransactionDetailItem(detail = detail)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSummaryItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun TransactionDetailItem(detail: TransactionDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // 날짜
            Text(
                text = "${detail.date.year}.${detail.date.monthNumber}.${detail.date.dayOfMonth}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            // 메모
            if (detail.memo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail.memo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
        }

        // 금액
        Text(
            text = "${Utils.formatAmount(detail.amount)}원",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
private fun CalendarDatePickerDialog(
    currentDate: LocalDate,
    maxDate: LocalDate? = null, // 선택 가능한 최대 날짜
    minDate: LocalDate? = null, // 선택 가능한 최소 날짜
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayMonth by remember { mutableStateOf(currentDate.monthNumber) }
    var displayYear by remember { mutableStateOf(currentDate.year) }
    var selectedDate by remember { mutableStateOf(currentDate) }
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
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
                            color = Color.Blue,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize
                        )
                    }

                    Text(
                        text = "${displayYear}년 ${displayMonth}월",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
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
                            color = Color.Blue,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Day of week headers
                val dayHeaders = listOf("일", "월", "화", "수", "목", "금", "토")
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
                                color = Color.Gray
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
                        val isSelected = date == selectedDate
                        val isToday = date == today

                        // 날짜 선택 가능 여부 체크
                        val isDisabled =
                            (maxDate != null && date > maxDate) || (minDate != null && date < minDate)

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clickable(enabled = !isDisabled) {
                                    if (!isDisabled) {
                                        selectedDate = date
                                    }
                                }
                                .background(
                                    when {
                                        isSelected -> Color.Blue
                                        isToday -> Color.LightGray
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
                                    isDisabled -> Color.LightGray // 선택 불가능한 날짜는 연한 회색
                                    isSelected -> Color.White
                                    isToday -> Color.Black
                                    else -> Color.Black
                                },
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소", color = Color.Black)
                    }

                    Button(
                        onClick = { onDateSelected(selectedDate) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) {
                        Text("확인", color = Color.White)
                    }
                }
            }
        }
    }
}