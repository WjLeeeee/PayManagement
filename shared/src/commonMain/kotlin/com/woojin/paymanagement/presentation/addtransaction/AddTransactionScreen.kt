package com.woojin.paymanagement.presentation.addtransaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.woojin.paymanagement.theme.BrandColor
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import kotlinx.datetime.DayOfWeek
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.BackHandler
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddTransactionScreen(
    transactions: List<Transaction>,
    selectedDate: LocalDate? = null,
    editTransaction: Transaction? = null,
    parsedTransaction: com.woojin.paymanagement.data.ParsedTransaction? = null,
    recurringTransaction: com.woojin.paymanagement.data.RecurringTransaction? = null,
    viewModel: AddTransactionViewModel,
    onSave: (List<Transaction>, String?) -> Unit,  // budgetExceededMessage 추가
    onCancel: () -> Unit
) {
    val strings = LocalStrings.current

    // 시스템 뒤로가기 버튼 처리
    BackHandler(onBack = onCancel)

    val uiState = viewModel.uiState
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(transactions, selectedDate, editTransaction, parsedTransaction, recurringTransaction) {
        if (recurringTransaction != null) {
            viewModel.initializeWithRecurringTransaction(transactions, recurringTransaction)
        } else if (parsedTransaction != null) {
            viewModel.initializeWithParsedTransaction(transactions, parsedTransaction)
        } else {
            viewModel.initialize(transactions, selectedDate, editTransaction)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
        // 상단 바: 왼쪽 닫기(취소) + 제목
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.cancel,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = if (uiState.isEditMode) strings.editTransaction else strings.addTransaction,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.offset(x = (-8).dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction Type Selection
        TransactionTypeSelector(
            selectedType = uiState.selectedType,
            onTypeSelected = viewModel::updateTransactionType
        )

        // Income Type Selection (only for income transactions)
        if (uiState.selectedType == TransactionType.INCOME) {
            Spacer(modifier = Modifier.height(16.dp))

            IncomeTypeSelector(
                selectedIncomeType = uiState.selectedIncomeType,
                onIncomeTypeSelected = viewModel::updateIncomeType,
                cardName = uiState.cardName,
                onCardNameChanged = viewModel::updateCardName,
                isChargingExistingBalanceCard = uiState.isChargingExistingBalanceCard,
                onChargingModeChanged = viewModel::updateChargingMode,
                availableBalanceCards = uiState.availableBalanceCards,
                selectedBalanceCardForCharge = uiState.selectedBalanceCardForCharge,
                onBalanceCardForChargeSelected = viewModel::updateSelectedBalanceCardForCharge,
                purchaseAmount = uiState.purchaseAmount,
                onPurchaseAmountChanged = viewModel::updatePurchaseAmount
            )
        }

        // Payment Method Selection (only for expense transactions)
        if (uiState.selectedType == TransactionType.EXPENSE) {
            Spacer(modifier = Modifier.height(16.dp))

            PaymentMethodSelector(
                selectedPaymentMethod = uiState.selectedPaymentMethod,
                onPaymentMethodSelected = viewModel::updatePaymentMethod,
                availableBalanceCards = uiState.availableBalanceCards,
                availableGiftCards = uiState.availableGiftCards,
                selectedBalanceCard = uiState.selectedBalanceCard,
                onBalanceCardSelected = viewModel::updateSelectedBalanceCard,
                selectedGiftCard = uiState.selectedGiftCard,
                onGiftCardSelected = viewModel::updateSelectedGiftCard,
                amount = uiState.amount.text,
                customPaymentMethods = uiState.customPaymentMethods,
                selectedCustomCardName = uiState.selectedCustomCardName,
                onCustomCardNameSelected = viewModel::updateSelectedCustomCardName
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Amount Input - 큰 숫자 + 밑줄 스타일
        SectionLabel(text = strings.transactionAmount)
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = uiState.amount,
            onValueChange = viewModel::updateAmount,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            ),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(BrandColor.mint),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f, fill = false)) {
                            if (uiState.amount.text.isEmpty()) {
                                Text(
                                    text = "0",
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                            innerTextField()
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.currencySymbol,
                            fontSize = 20.sp,
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

        Spacer(modifier = Modifier.height(10.dp))

        // 금액 퀵 버튼 (+1천, +5천, +1만, +5만)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1_000L, 5_000L, 10_000L, 50_000L).forEach { quickAmount ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { viewModel.addQuickAmount(quickAmount) }
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.quickAmountLabel(quickAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Settlement Section (for expense only)
        if (uiState.selectedType == TransactionType.EXPENSE) {
            Spacer(modifier = Modifier.height(20.dp))

            SettlementSection(
                isSettlement = uiState.isSettlement,
                onSettlementChange = viewModel::updateSettlement,
                settlementAmount = uiState.settlementAmount,
                onSettlementAmountChange = viewModel::updateSettlementAmount
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Category Selection
        CategoryChipGrid(
            categories = uiState.categories,
            selectedCategory = uiState.category,
            onCategorySelected = viewModel::updateCategory,
            transactionType = uiState.selectedType,
            uiState = uiState,
            selectedSubCategory = uiState.subCategory,
            onSubCategorySelected = viewModel::updateSubCategory
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 날짜 · 사용처 · 메모를 한 장의 카드로 묶어서 표시
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp)
        ) {
            // Date (표시 전용, 기존과 동일하게 읽기 전용)
            FormCardRow(emoji = "📅", label = strings.dateLabel) {
                Text(
                    text = uiState.date?.let { date ->
                        val weekday = when (date.dayOfWeek) {
                            DayOfWeek.MONDAY -> strings.monday
                            DayOfWeek.TUESDAY -> strings.tuesday
                            DayOfWeek.WEDNESDAY -> strings.wednesday
                            DayOfWeek.THURSDAY -> strings.thursday
                            DayOfWeek.FRIDAY -> strings.friday
                            DayOfWeek.SATURDAY -> strings.saturday
                            else -> strings.sunday
                        }
                        "${strings.fullDate(date.year, date.monthNumber, date.dayOfMonth)} (${strings.weekdayShort(weekday)})"
                    } ?: "",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Merchant Input (지출일 때만 표시)
            if (uiState.selectedType == TransactionType.EXPENSE) {
                val suggestionRequester = remember { BringIntoViewRequester() }
                LaunchedEffect(uiState.merchantSuggestions) {
                    if (uiState.merchantSuggestions.isNotEmpty()) {
                        suggestionRequester.bringIntoView()
                    }
                }
                FormCardDivider()
                FormCardRow(emoji = "🏪", label = strings.merchantLabel) {
                    FormCardTextField(
                        value = uiState.merchant,
                        onValueChange = viewModel::updateMerchant,
                        placeholder = strings.merchantLabel,
                        singleLine = true
                    )
                }
                // 자동 추천: 사용처 줄 바로 아래, 입력값과 같은 시작선에 칩으로 표시
                if (uiState.merchantSuggestions.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = FormCardValueStart, bottom = 12.dp)
                            .bringIntoViewRequester(suggestionRequester),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.merchantSuggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                                    .clickable { viewModel.selectMerchantSuggestion(suggestion) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Memo Input
            FormCardDivider()
            FormCardRow(emoji = "📝", label = strings.memo) {
                FormCardTextField(
                    value = uiState.memo,
                    onValueChange = viewModel::updateMemo,
                    placeholder = strings.memoOptional,
                    singleLine = false,
                    maxLines = 3
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 공유방 참여 중일 때 저장 대상 선택
        if (uiState.isInSharedRoom) {
            SaveTargetSelector(
                selected = uiState.saveTarget,
                onSelect = viewModel::updateSaveTarget
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Save Button (취소는 상단 왼쪽 X 버튼)
        Button(
            onClick = {
                scope.launch {
                    val result = viewModel.saveTransaction()
                    if (result.transactions.isNotEmpty()) {
                        // 화면을 바로 닫고 부모 화면에 예산 초과 메시지 전달
                        onSave(result.transactions, result.budgetExceededMessage)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = uiState.saveEnabled && !uiState.isLoading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandColor.mint,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                text = if (uiState.isLoading) strings.savingTransaction else strings.save,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Error display
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        }
    }
}

@Composable
private fun SaveTargetSelector(
    selected: SaveTarget,
    onSelect: (SaveTarget) -> Unit
) {
    val options = listOf(
        SaveTarget.PERSONAL_ONLY to "개인만",
        SaveTarget.SHARED_ONLY to "공유만",
        SaveTarget.BOTH to "둘 다"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(text = "저장 대상")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            options.forEach { (target, label) ->
                val isSelected = selected == target
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isSelected) Modifier.shadow(1.dp, RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surface
                            else Color.Transparent
                        )
                        .clickable { onSelect(target) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BrandColor.mint
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---- 날짜 · 사용처 · 메모 카드 구성 요소 ----

private val FormCardEmojiWidth = 22.dp
private val FormCardLabelWidth = 64.dp
private val FormCardGap = 12.dp
/** 값(입력창)이 시작되는 위치 - 자동 추천 칩도 이 선에 맞춤 */
private val FormCardValueStart = FormCardEmojiWidth + FormCardGap + FormCardLabelWidth + FormCardGap

@Composable
private fun FormCardRow(
    emoji: String,
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .padding(vertical = 12.dp),
        // 아이콘 · 라벨 · 값을 세로 중앙에 맞춤 (메모가 여러 줄이 되면 그 높이 기준 중앙)
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 17.sp,
            modifier = Modifier.width(FormCardEmojiWidth)
        )
        Spacer(modifier = Modifier.width(FormCardGap))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(FormCardLabelWidth)
        )
        Spacer(modifier = Modifier.width(FormCardGap))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            content()
        }
    }
}

@Composable
private fun FormCardDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    )
}

@Composable
private fun FormCardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    maxLines: Int = 1
) {
    val textStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else maxLines,
        textStyle = textStyle,
        cursorBrush = SolidColor(BrandColor.mint),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.outline
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

