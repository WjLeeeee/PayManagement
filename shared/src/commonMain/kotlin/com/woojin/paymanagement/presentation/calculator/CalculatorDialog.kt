package com.woojin.paymanagement.presentation.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

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
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var calculatorResult by remember { mutableStateOf<CalculatorResult?>(null) }

    // 날짜 선택 다이얼로그 상태
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // 사용 가능한 카테고리 목록
    val availableCategories = remember(transactions, selectedTransactionType) {
        calculatorUseCase.getAvailableCategories(transactions, selectedTransactionType)
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
                    .padding(24.dp)
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
                        .verticalScroll(rememberScrollState())
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
                            Text("시작일", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(
                                "${startDate.year}.${startDate.monthNumber}.${startDate.dayOfMonth}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                        }

                        Text("~", modifier = Modifier.padding(horizontal = 8.dp), color = Color.Black)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEndDatePicker = true }
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text("종료일", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                                selectedCategories = emptySet()
                            },
                            label = { Text("수입", color = if (selectedTransactionType == TransactionType.INCOME) Color.White else Color.Black) },
                            selected = selectedTransactionType == TransactionType.INCOME,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.Blue,
                                selectedLabelColor = Color.White
                            )
                        )

                        FilterChip(
                            onClick = {
                                selectedTransactionType = TransactionType.EXPENSE
                                selectedCategories = emptySet()
                            },
                            label = { Text("지출", color = if (selectedTransactionType == TransactionType.EXPENSE) Color.White else Color.Black) },
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
                        text = "카테고리 선택",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "선택하지 않으면 모든 카테고리가 포함됩니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 카테고리 목록
                    if (availableCategories.isNotEmpty()) {
                        availableCategories.chunked(2).forEach { categoryPair ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                categoryPair.forEach { category ->
                                    FilterChip(
                                        onClick = {
                                            selectedCategories = if (selectedCategories.contains(category)) {
                                                selectedCategories - category
                                            } else {
                                                selectedCategories + category
                                            }
                                        },
                                        label = { Text(category, color = if (selectedCategories.contains(category)) Color.White else Color.Black) },
                                        selected = selectedCategories.contains(category),
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color.Gray,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }

                                // 홀수 개일 때 빈 공간 채우기
                                if (categoryPair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    } else {
                        Text(
                            text = "해당 조건에 맞는 카테고리가 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 계산 결과
                    calculatorResult?.let { result ->
                        CalculatorResultCard(result = result)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("닫기", color = Color.Black)
                    }

                    Button(
                        onClick = {
                            val request = CalculatorRequest(
                                startDate = startDate,
                                endDate = endDate,
                                transactionType = selectedTransactionType,
                                categories = selectedCategories.toList()
                            )
                            calculatorResult = calculatorUseCase.calculate(transactions, request)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) {
                        Text("계산", color = Color.White)
                    }
                }
            }
        }

        // 날짜 선택 다이얼로그들
        if (showStartDatePicker) {
            CalendarDatePickerDialog(
                currentDate = startDate,
                onDateSelected = { newDate ->
                    startDate = newDate
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false }
            )
        }

        if (showEndDatePicker) {
            CalendarDatePickerDialog(
                currentDate = endDate,
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
        colors = CardDefaults.cardColors(containerColor = Color.LightGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "계산 결과",
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

            // 카테고리별 세부 내용
            if (result.categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "카테고리별 상세",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                result.categories.take(5).forEach { category ->
                    CategoryResultItem(category = category)
                    Spacer(modifier = Modifier.height(4.dp))
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
private fun CategoryResultItem(category: CategorySummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = "${category.transactionCount}건",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${Utils.formatAmount(category.amount)}원",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "${(category.percentage * 10).toInt() / 10.0}%",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun CalendarDatePickerDialog(
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayMonth by remember { mutableStateOf(currentDate.monthNumber) }
    var displayYear by remember { mutableStateOf(currentDate.year) }
    var selectedDate by remember { mutableStateOf(currentDate) }

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
                        Text("◀", color = Color.Blue, fontSize = MaterialTheme.typography.titleLarge.fontSize)
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
                        Text("▶", color = Color.Blue, fontSize = MaterialTheme.typography.titleLarge.fontSize)
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
                        val isToday = date == Clock.System.todayIn(TimeZone.currentSystemDefault())

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clickable { selectedDate = date }
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