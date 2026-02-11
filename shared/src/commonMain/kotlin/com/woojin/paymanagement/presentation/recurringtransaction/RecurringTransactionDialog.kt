package com.woojin.paymanagement.presentation.recurringtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.woojin.paymanagement.data.*
import com.woojin.paymanagement.strings.LocalStrings
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

// 콤마 포맷팅 함수
private fun formatNumberWithComma(value: String): String {
    if (value.isEmpty()) return ""
    val number = value.replace(",", "")
    if (number.isEmpty() || !number.all { it.isDigit() }) return value

    return number.reversed().chunked(3).joinToString(",").reversed()
}

// 콤마 제거 함수
private fun removeComma(value: String): String {
    return value.replace(",", "")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurringTransactionDialog(
    transaction: RecurringTransaction?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (RecurringTransaction) -> Unit
) {
    val strings = LocalStrings.current
    // 초기 금액 설정 (소수점 제거 및 콤마 추가)
    val initialAmount = transaction?.amount?.toInt()?.toString() ?: ""
    val formattedInitialAmount = formatNumberWithComma(initialAmount)

    var selectedType by remember { mutableStateOf(transaction?.type ?: TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(transaction?.category ?: "") }
    var amount by remember {
        mutableStateOf(
            TextFieldValue(
                text = formattedInitialAmount,
                selection = TextRange(formattedInitialAmount.length)
            )
        )
    }
    var merchant by remember { mutableStateOf(transaction?.merchant ?: "") }
    var memo by remember { mutableStateOf(transaction?.memo ?: "") }
    var selectedPaymentMethod by remember { mutableStateOf(transaction?.paymentMethod ?: PaymentMethod.CASH) }
    var selectedPattern by remember { mutableStateOf(transaction?.pattern ?: RecurringPattern.MONTHLY) }
    var dayOfMonth by remember { mutableStateOf(transaction?.dayOfMonth ?: 1) }
    var dayOfWeek by remember { mutableStateOf(transaction?.dayOfWeek ?: 1) }
    var selectedWeekendHandling by remember { mutableStateOf(transaction?.weekendHandling ?: com.woojin.paymanagement.data.WeekendHandling.AS_IS) }

    // 카테고리 목록 필터링
    val filteredCategories = categories.filter { it.type == selectedType }

    // 카테고리가 변경되었을 때 초기화
    LaunchedEffect(selectedType) {
        if (selectedCategory.isEmpty() || !filteredCategories.any { it.name == selectedCategory }) {
            selectedCategory = filteredCategories.firstOrNull()?.name ?: ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header
                Text(
                    text = if (transaction == null) strings.addRecurringTransaction else strings.editRecurringTransaction,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // 거래 타입 선택
                Text(
                    text = strings.transactionType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { selectedType = TransactionType.INCOME },
                        label = {
                            Text(
                                strings.income,
                                color = if (selectedType == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        selected = selectedType == TransactionType.INCOME,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        onClick = { selectedType = TransactionType.EXPENSE },
                        label = {
                            Text(
                                strings.expense,
                                color = if (selectedType == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        selected = selectedType == TransactionType.EXPENSE,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                HorizontalDivider()

                // 카테고리 선택
                Text(
                    text = strings.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 카테고리 목록 (계산기 스타일)
                if (filteredCategories.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        filteredCategories.forEach { category ->
                            val isSelected = category.name == selectedCategory
                            val backgroundColor = when {
                                isSelected && selectedType == TransactionType.INCOME -> Color(0xFFE3F2FD) // 연한 파랑
                                isSelected && selectedType == TransactionType.EXPENSE -> Color(0xFFFFEBEE) // 연한 빨강
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val borderColor = when {
                                isSelected && selectedType == TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                                isSelected && selectedType == TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                                else -> Color.Transparent
                            }
                            val textColor = when {
                                isSelected -> Color.Black
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                                    .clickable { selectedCategory = category.name }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = category.emoji,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = strings.noCategoriesRegistered,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                // 금액 입력
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        // 콤마 제거 후 숫자만 남기기
                        val digitsOnly = removeComma(newValue.text)

                        if (digitsOnly.isEmpty() || digitsOnly.all { it.isDigit() }) {
                            // 콤마 추가
                            val formatted = formatNumberWithComma(digitsOnly)

                            // 커서를 오른쪽 끝으로 이동
                            amount = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                        }
                    },
                    label = { Text(strings.transactionAmount) },
                    suffix = { Text(strings.currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (selectedType == TransactionType.INCOME)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        focusedLabelColor = if (selectedType == TransactionType.INCOME)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                )

                // 사용처 입력 (필수)
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(strings.merchantLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (selectedType == TransactionType.INCOME)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        focusedLabelColor = if (selectedType == TransactionType.INCOME)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                )

                // 메모 입력 (선택)
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text(strings.memoOptionalShort) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (selectedType == TransactionType.INCOME)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        focusedLabelColor = if (selectedType == TransactionType.INCOME)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                )

                HorizontalDivider()

                // 결제 수단 선택 (지출일 때만)
                if (selectedType == TransactionType.EXPENSE) {
                    Text(
                        text = strings.paymentMethod,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            PaymentMethod.CASH to strings.cashCheckCard,
                            PaymentMethod.CARD to strings.creditCard,
                            PaymentMethod.BALANCE_CARD to strings.balanceCard,
                            PaymentMethod.GIFT_CARD to strings.giftCard
                        ).forEach { (method, label) ->
                            val isSelected = selectedPaymentMethod == method
                            val backgroundColor = when {
                                isSelected -> Color(0xFFFFEBEE) // 연한 빨강
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val borderColor = when {
                                isSelected -> MaterialTheme.colorScheme.error
                                else -> Color.Transparent
                            }
                            val textColor = when {
                                isSelected -> Color.Black
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                                    .clickable { selectedPaymentMethod = method }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                }

                // 반복 패턴 선택
                Text(
                    text = strings.recurringPatternLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { selectedPattern = RecurringPattern.MONTHLY },
                        label = { Text(strings.monthly) },
                        selected = selectedPattern == RecurringPattern.MONTHLY,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        onClick = { selectedPattern = RecurringPattern.WEEKLY },
                        label = { Text(strings.weekly) },
                        selected = selectedPattern == RecurringPattern.WEEKLY,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // 날짜 선택
                if (selectedPattern == RecurringPattern.MONTHLY) {
                    Text(
                        text = strings.whichDayOfMonth,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (dayOfMonth > 1) dayOfMonth-- },
                            enabled = dayOfMonth > 1
                        ) {
                            Text("-", style = MaterialTheme.typography.titleLarge)
                        }

                        Text(
                            text = strings.dayOfMonth(dayOfMonth),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        IconButton(
                            onClick = { if (dayOfMonth < 31) dayOfMonth++ },
                            enabled = dayOfMonth < 31
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    // 주말 처리 방식 (매달 패턴일 때만 표시)
                    Text(
                        text = strings.weekendHandling,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        listOf(
                            com.woojin.paymanagement.data.WeekendHandling.AS_IS to strings.applyAsIs,
                            com.woojin.paymanagement.data.WeekendHandling.PREVIOUS_WEEKDAY to strings.moveToPreviousWeekday,
                            com.woojin.paymanagement.data.WeekendHandling.NEXT_WEEKDAY to strings.moveToNextWeekday
                        ).forEach { (handling, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedWeekendHandling = handling },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = selectedWeekendHandling == handling,
                                    onClick = { selectedWeekendHandling = handling },
                                    modifier = Modifier.size(40.dp),
                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                        selectedColor = if (selectedType == TransactionType.INCOME)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                } else {
                    Text(
                        text = strings.whichDayOfWeek,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            1 to strings.monday,
                            2 to strings.tuesday,
                            3 to strings.wednesday,
                            4 to strings.thursday,
                            5 to strings.friday,
                            6 to strings.saturday,
                            7 to strings.sunday
                        ).forEach { (value, label) ->
                            val isSelected = dayOfWeek == value
                            val backgroundColor = when {
                                isSelected && selectedType == TransactionType.INCOME -> Color(0xFFE3F2FD) // 연한 파랑
                                isSelected && selectedType == TransactionType.EXPENSE -> Color(0xFFFFEBEE) // 연한 빨강
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val borderColor = when {
                                isSelected && selectedType == TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                                isSelected && selectedType == TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                                else -> Color.Transparent
                            }
                            val textColor = when {
                                isSelected -> Color.Black
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                                    .clickable { dayOfWeek = value }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.cancel)
                    }

                    Button(
                        onClick = {
                            // 콤마 제거 후 Double로 변환
                            val amountValue = removeComma(amount.text).toDoubleOrNull() ?: 0.0
                            if (amountValue > 0 && selectedCategory.isNotEmpty() && merchant.isNotEmpty()) {
                                val newTransaction = RecurringTransaction(
                                    id = transaction?.id ?: kotlin.random.Random.nextLong().toString(),
                                    type = selectedType,
                                    category = selectedCategory,
                                    amount = amountValue,
                                    merchant = merchant,
                                    memo = memo,
                                    paymentMethod = selectedPaymentMethod,
                                    balanceCardId = transaction?.balanceCardId,
                                    giftCardId = transaction?.giftCardId,
                                    pattern = selectedPattern,
                                    dayOfMonth = if (selectedPattern == RecurringPattern.MONTHLY) dayOfMonth else null,
                                    dayOfWeek = if (selectedPattern == RecurringPattern.WEEKLY) dayOfWeek else null,
                                    weekendHandling = selectedWeekendHandling,
                                    isActive = transaction?.isActive ?: true,
                                    createdAt = transaction?.createdAt ?: Clock.System.now().toEpochMilliseconds(),
                                    lastExecutedDate = transaction?.lastExecutedDate
                                )
                                onSave(newTransaction)
                            }
                        },
                        enabled = removeComma(amount.text).toDoubleOrNull() != null &&
                                removeComma(amount.text).toDoubleOrNull()!! > 0 &&
                                selectedCategory.isNotEmpty() &&
                                merchant.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (transaction == null) strings.add else strings.edit)
                    }
                }
            }
        }
    }
}
