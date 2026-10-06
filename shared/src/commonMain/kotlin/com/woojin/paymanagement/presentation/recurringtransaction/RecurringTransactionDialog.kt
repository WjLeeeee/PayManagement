package com.woojin.paymanagement.presentation.recurringtransaction

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.woojin.paymanagement.theme.BrandColor
import kotlinx.coroutines.delay
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
import com.woojin.paymanagement.theme.SavingColor
import com.woojin.paymanagement.theme.InvestmentColor
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionDialog(
    transaction: RecurringTransaction?,
    categories: List<Category>,
    customPaymentMethods: List<CustomPaymentMethod> = emptyList(),
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
    var selectedCardName by remember {
        mutableStateOf(
            transaction?.cardName ?: (customPaymentMethods.find { it.isDefault } ?: customPaymentMethods.firstOrNull())?.name
        )
    }
    var selectedPattern by remember { mutableStateOf(transaction?.pattern ?: RecurringPattern.MONTHLY) }
    var dayOfMonth by remember { mutableStateOf(transaction?.dayOfMonth ?: 1) }
    var dayOfWeek by remember { mutableStateOf(transaction?.dayOfWeek ?: 1) }
    var selectedWeekendHandling by remember { mutableStateOf(transaction?.weekendHandling ?: com.woojin.paymanagement.data.WeekendHandling.AS_IS) }
    var includeWeekends by remember { mutableStateOf(transaction?.includeWeekends ?: true) }

    // 카테고리 목록 필터링
    val filteredCategories = categories.filter { it.type == selectedType }

    // 카테고리가 변경되었을 때 초기화
    LaunchedEffect(selectedType) {
        if (selectedCategory.isEmpty() || !filteredCategories.any { it.name == selectedCategory }) {
            selectedCategory = filteredCategories.firstOrNull()?.name ?: ""
        }
    }

    val typeColor = when (selectedType) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.SAVING -> SavingColor.color
        TransactionType.INVESTMENT -> InvestmentColor.color
    }
    val typeLightColor = when (selectedType) {
        TransactionType.INCOME -> Color(0xFFE3F2FD) // 연한 파랑
        TransactionType.EXPENSE -> Color(0xFFFFEBEE) // 연한 빨강
        TransactionType.SAVING -> SavingColor.lightBackground
        TransactionType.INVESTMENT -> InvestmentColor.lightBackground
    }

    // 메모 섹션 펼침 상태 - 화면 안에서만 기억. 내용이 있으면 펼친 채로 시작
    var memoExpanded by remember { mutableStateOf(memo.isNotBlank()) }

    val canSave = removeComma(amount.text).toDoubleOrNull() != null &&
                                removeComma(amount.text).toDoubleOrNull()!! > 0 &&
                                selectedCategory.isNotEmpty() &&
                                (selectedType != TransactionType.EXPENSE || merchant.isNotEmpty())

    // 다이얼로그 구조는 유지, 화면 전체를 덮도록 표시
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .imePadding()
            ) {
                // 상단 바: 왼쪽 X(취소) + 제목
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancel,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (transaction == null) strings.addRecurringTransaction else strings.editRecurringTransaction,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ① 어떤 거래인가요? (유형 + 카테고리)
                    RecurringSectionCard(number = 1, title = strings.recurringSectionWhat) {
                        // 거래 유형 세그먼트 (유형 색 유지)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp)
                        ) {
                            listOf(
                                TransactionType.INCOME to strings.income,
                                TransactionType.EXPENSE to strings.expense,
                                TransactionType.SAVING to strings.saving,
                                TransactionType.INVESTMENT to strings.investment
                            ).forEach { (type, label) ->
                                val isSelected = selectedType == type
                                val color = when (type) {
                                    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                                    TransactionType.SAVING -> SavingColor.color
                                    TransactionType.INVESTMENT -> InvestmentColor.color
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { selectedType = type }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 카테고리 (선택 시 유형 색)
                        if (filteredCategories.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                filteredCategories.forEach { category ->
                                    RecurringPillChip(
                                        label = category.name,
                                        emoji = category.emoji,
                                        selected = category.name == selectedCategory,
                                        selectedColor = typeColor,
                                        selectedBackground = typeLightColor,
                                        onClick = { selectedCategory = category.name }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = strings.noCategoriesRegistered,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // ② 얼마인가요?
                    RecurringSectionCard(number = 2, title = strings.recurringSectionAmount) {
                        BasicTextField(
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(BrandColor.mint),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.weight(1f, fill = false)) {
                                            if (amount.text.isEmpty()) {
                                                Text(
                                                    text = "0",
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            }
                                            innerTextField()
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = strings.currencySymbol,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(BrandColor.mint)
                                    )
                                }
                            }
                        )
                    }

                    // ③ 언제 반복할까요?
                    RecurringSectionCard(number = 3, title = strings.recurringSectionWhen) {
                        // 반복 패턴 세그먼트
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp)
                        ) {
                            listOf(
                                RecurringPattern.MONTHLY to strings.monthly,
                                RecurringPattern.WEEKLY to strings.weekly,
                                RecurringPattern.DAILY to strings.daily
                            ).forEach { (pattern, label) ->
                                val isSelected = selectedPattern == pattern
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { selectedPattern = pattern }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BrandColor.mint else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 날짜 선택
                        if (selectedPattern == RecurringPattern.DAILY) {
                            RecurringSubLabel(strings.includeWeekendsOptionLabel)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                listOf(
                                    true to strings.includeWeekendsOption,
                                    false to strings.excludeWeekendsOption
                                ).forEach { (value, label) ->
                                    RecurringPillChip(
                                        label = label,
                                        selected = includeWeekends == value,
                                        onClick = { includeWeekends = value }
                                    )
                                }
                            }
                        } else if (selectedPattern == RecurringPattern.MONTHLY) {
                            // 매달 몇 일? (- / +)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StepperButton(
                                    text = "−",
                                    enabled = dayOfMonth > 1,
                                    onClick = { if (dayOfMonth > 1) dayOfMonth-- }
                                )
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.monthly,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.dayOfMonth(dayOfMonth),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandColor.mint
                                    )
                                }
                                StepperButton(
                                    text = "+",
                                    enabled = dayOfMonth < 31,
                                    onClick = { if (dayOfMonth < 31) dayOfMonth++ }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 주말 처리 방식 (매달 패턴일 때만 표시)
                            RecurringSubLabel(strings.weekendHandling)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                listOf(
                                    com.woojin.paymanagement.data.WeekendHandling.AS_IS to strings.applyAsIs,
                                    com.woojin.paymanagement.data.WeekendHandling.PREVIOUS_WEEKDAY to strings.moveToPreviousWeekday,
                                    com.woojin.paymanagement.data.WeekendHandling.NEXT_WEEKDAY to strings.moveToNextWeekday
                                ).forEach { (handling, label) ->
                                    RecurringPillChip(
                                        label = label,
                                        selected = selectedWeekendHandling == handling,
                                        onClick = { selectedWeekendHandling = handling }
                                    )
                                }
                            }
                        } else {
                            RecurringSubLabel(strings.whichDayOfWeek)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
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
                                    RecurringPillChip(
                                        label = label,
                                        selected = dayOfWeek == value,
                                        onClick = { dayOfWeek = value }
                                    )
                                }
                            }
                        }
                    }

                    // ④ 어디서 결제하나요? (지출일 때만: 사용처 필수 + 결제 수단)
                    if (selectedType == TransactionType.EXPENSE) {
                        RecurringSectionCard(number = 4, title = strings.recurringSectionWhere) {
                            OutlinedTextField(
                                value = merchant,
                                onValueChange = { merchant = it },
                                placeholder = { Text(strings.merchantLabel) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = recurringFieldColors()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                listOf(
                                    PaymentMethod.CASH to strings.cashCheckCard,
                                    PaymentMethod.CARD to strings.creditCard,
                                    PaymentMethod.BALANCE_CARD to strings.balanceCard,
                                    PaymentMethod.GIFT_CARD to strings.giftCard
                                ).forEach { (method, label) ->
                                    RecurringPillChip(
                                        label = label,
                                        selected = selectedPaymentMethod == method,
                                        onClick = {
                                            selectedPaymentMethod = method
                                            if (method != PaymentMethod.CARD) selectedCardName = null
                                        }
                                    )
                                }
                            }

                            // 카드 선택 시 커스텀 카드 드롭다운
                            if (selectedPaymentMethod == PaymentMethod.CARD && customPaymentMethods.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))

                                var cardDropdownExpanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = cardDropdownExpanded,
                                    onExpandedChange = { cardDropdownExpanded = !cardDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedCardName ?: customPaymentMethods.firstOrNull()?.name ?: "",
                                        onValueChange = { },
                                        readOnly = true,
                                        label = { Text(strings.selectCard) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardDropdownExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = recurringFieldColors()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = cardDropdownExpanded,
                                        onDismissRequest = { cardDropdownExpanded = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        customPaymentMethods.forEach { method ->
                                            DropdownMenuItem(
                                                text = { Text(method.name, color = MaterialTheme.colorScheme.onSurface) },
                                                onClick = {
                                                    selectedCardName = method.name
                                                    cardDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 메모 (선택, 접기)
                    RecurringSectionCard(
                        number = if (selectedType == TransactionType.EXPENSE) 5 else 4,
                        title = strings.memo,
                        optional = true,
                        expanded = memoExpanded,
                        onToggle = { memoExpanded = !memoExpanded },
                        summary = memo.ifBlank { strings.noMemo }
                    ) {
                        OutlinedTextField(
                            value = memo,
                            onValueChange = { memo = it },
                            placeholder = { Text(strings.enterMemo) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = recurringFieldColors()
                        )
                    }
                }

                // 하단 저장 버튼
                Button(
                    onClick = {
                            // 콤마 제거 후 Double로 변환
                            val amountValue = removeComma(amount.text).toDoubleOrNull() ?: 0.0
                            val isMerchantValid = selectedType != TransactionType.EXPENSE || merchant.isNotEmpty()
                            if (amountValue > 0 && selectedCategory.isNotEmpty() && isMerchantValid) {
                                val newTransaction = RecurringTransaction(
                                    id = transaction?.id ?: kotlin.random.Random.nextLong().toString(),
                                    type = selectedType,
                                    category = selectedCategory,
                                    amount = amountValue,
                                    merchant = if (selectedType == TransactionType.SAVING || selectedType == TransactionType.INVESTMENT) "" else merchant,
                                    memo = memo,
                                    paymentMethod = if (selectedType == TransactionType.SAVING || selectedType == TransactionType.INVESTMENT) PaymentMethod.CASH else selectedPaymentMethod,
                                    balanceCardId = transaction?.balanceCardId,
                                    giftCardId = transaction?.giftCardId,
                                    cardName = if (selectedType != TransactionType.SAVING && selectedType != TransactionType.INVESTMENT && selectedPaymentMethod == PaymentMethod.CARD) selectedCardName else null,
                                    pattern = selectedPattern,
                                    dayOfMonth = if (selectedPattern == RecurringPattern.MONTHLY) dayOfMonth else null,
                                    dayOfWeek = if (selectedPattern == RecurringPattern.WEEKLY) dayOfWeek else null,
                                    weekendHandling = selectedWeekendHandling,
                                    includeWeekends = includeWeekends,
                                    isActive = transaction?.isActive ?: true,
                                    createdAt = transaction?.createdAt ?: Clock.System.now().toEpochMilliseconds(),
                                    lastExecutedDate = transaction?.lastExecutedDate
                                )
                                onSave(newTransaction)
                            }
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandColor.mint,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = if (transaction == null) strings.add else strings.edit,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private const val RecurringAnimMs = 280

/**
 * 번호가 붙은 섹션 카드 (예산 카테고리 추가와 같은 방식)
 * - 필수(optional = false): 번호 민트, 항상 펼침
 * - 선택(optional = true): 번호 회색, 헤더를 눌러 접기/펼치기, 접혀 있을 때 요약 표시
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecurringSectionCard(
    number: Int,
    title: String,
    optional: Boolean = false,
    expanded: Boolean = true,
    onToggle: () -> Unit = {},
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(RecurringAnimMs),
        label = "recurringSectionArrow"
    )

    // 선택 섹션을 펼치면 펼쳐지는 만큼 같이 스크롤해서 카드 전체가 보이게 함
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(expanded) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }
        if (optional && expanded) {
            val steps = 6
            repeat(steps) {
                delay((RecurringAnimMs / steps).toLong())
                bringIntoViewRequester.bringIntoView()
            }
            delay(40)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (optional) Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onToggle) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (optional) MaterialTheme.colorScheme.surfaceVariant else BrandColor.mint),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (optional) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (optional) {
                // 접혀 있을 때만 요약 표시
                if (!expanded && summary != null) {
                    Text(
                        text = summary,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 140.dp)
                            .padding(start = 8.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .rotate(arrowRotation)
                )
            }
        }
        AnimatedVisibility(
            visible = !optional || expanded,
            enter = expandVertically(tween(RecurringAnimMs), expandFrom = Alignment.Top) + fadeIn(tween(RecurringAnimMs)),
            exit = shrinkVertically(tween(RecurringAnimMs), shrinkTowards = Alignment.Top) + fadeOut(tween(RecurringAnimMs / 2))
        ) {
            Column(modifier = Modifier.padding(top = 14.dp), content = content)
        }
    }
}

/** 알약 모양 선택 칩 - 기본은 민트, 카테고리는 유형 색 */
@Composable
private fun RecurringPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    emoji: String? = null,
    selectedColor: Color = BrandColor.mint,
    selectedBackground: Color = BrandColor.mint.copy(alpha = 0.12f)
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) selectedBackground else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) selectedColor else Color.Transparent,
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
            color = when {
                !selected -> MaterialTheme.colorScheme.onSurfaceVariant
                selectedColor == BrandColor.mint -> BrandColor.mint
                else -> Color.Black
            }
        )
    }
}

@Composable
private fun RecurringSubLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun StepperButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

/** 테두리 없는 회색 채움형 입력창 (포커스 시 민트 테두리) */
@Composable
private fun recurringFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedBorderColor = BrandColor.mint,
    unfocusedBorderColor = Color.Transparent,
    focusedLabelColor = BrandColor.mint,
    cursorColor = BrandColor.mint
)
