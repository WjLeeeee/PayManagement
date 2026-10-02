package com.woojin.paymanagement.presentation.addtransaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.CustomPaymentMethod
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.strings.LocalStrings

@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    // 세그먼트 컨트롤: 회색 트랙 위에 선택된 유형만 흰 버튼으로 떠 보이게 표시
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
            .selectableGroup()
    ) {
        TransactionType.values().forEach { type ->
            val isSelected = type == selectedType
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isSelected) Modifier.shadow(1.dp, RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        onClick = { onTypeSelected(type) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (type) {
                        TransactionType.INCOME -> strings.income
                        TransactionType.EXPENSE -> strings.expense
                        TransactionType.SAVING -> strings.saving
                        TransactionType.INVESTMENT -> strings.investment
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) transactionTypeColor(type)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomeTypeSelector(
    selectedIncomeType: IncomeType,
    onIncomeTypeSelected: (IncomeType) -> Unit,
    cardName: String,
    onCardNameChanged: (String) -> Unit,
    isChargingExistingBalanceCard: Boolean,
    onChargingModeChanged: (Boolean) -> Unit,
    availableBalanceCards: List<BalanceCard>,
    selectedBalanceCardForCharge: BalanceCard?,
    onBalanceCardForChargeSelected: (BalanceCard?) -> Unit,
    purchaseAmount: TextFieldValue,
    onPurchaseAmountChanged: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(modifier = modifier) {
        SectionLabel(text = strings.incomeType)

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IncomeType.values().forEach { incomeType ->
                SelectablePillChip(
                    label = when (incomeType) {
                        IncomeType.CASH -> strings.cash
                        IncomeType.BALANCE_CARD -> strings.balanceCard
                        IncomeType.GIFT_CARD -> strings.giftCard
                    },
                    emoji = when (incomeType) {
                        IncomeType.CASH -> "💵"
                        IncomeType.BALANCE_CARD -> "🎫"
                        IncomeType.GIFT_CARD -> "🎁"
                    },
                    selected = incomeType == selectedIncomeType,
                    onClick = { onIncomeTypeSelected(incomeType) }
                )
            }
        }

        // 잔액권 선택 시
        if (selectedIncomeType == IncomeType.BALANCE_CARD) {
            Spacer(modifier = Modifier.height(8.dp))

            // 기존 잔액권이 있을 때만 선택 옵션 표시
            if (availableBalanceCards.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectablePillChip(
                        label = strings.newBalanceCard,
                        selected = !isChargingExistingBalanceCard,
                        onClick = { onChargingModeChanged(false) }
                    )
                    SelectablePillChip(
                        label = strings.chargeExistingBalanceCard,
                        selected = isChargingExistingBalanceCard,
                        onClick = { onChargingModeChanged(true) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 입력 필드 또는 드롭다운
            if (availableBalanceCards.isEmpty() || !isChargingExistingBalanceCard) {
                // 잔액권이 없거나 새로 추가 선택 시 - 이름 입력
                OutlinedTextField(
                    value = cardName,
                    onValueChange = onCardNameChanged,
                    label = {
                        Text(
                            text = strings.balanceCardNameHint,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                // 기존 잔액권 충전 선택 시 - 드롭다운
                CardSelectionDropdown(
                    cards = availableBalanceCards,
                    selectedCard = selectedBalanceCardForCharge,
                    onCardSelected = onBalanceCardForChargeSelected,
                    label = strings.selectBalanceCardToCharge
                )
            }

            // 할인 구매 금액 입력 (선택)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = purchaseAmount,
                onValueChange = onPurchaseAmountChanged,
                label = {
                    Text(
                        text = strings.purchaseAmountLabel,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                placeholder = {
                    Text(
                        text = strings.purchaseAmountHint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // 상품권 선택 시 - 기존 로직 유지
        if (selectedIncomeType == IncomeType.GIFT_CARD) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = cardName,
                onValueChange = onCardNameChanged,
                label = {
                    Text(
                        text = strings.giftCardNameHint,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentMethodSelector(
    selectedPaymentMethod: PaymentMethod,
    onPaymentMethodSelected: (PaymentMethod) -> Unit,
    availableBalanceCards: List<BalanceCard>,
    availableGiftCards: List<GiftCard>,
    selectedBalanceCard: BalanceCard?,
    onBalanceCardSelected: (BalanceCard?) -> Unit,
    selectedGiftCard: GiftCard?,
    onGiftCardSelected: (GiftCard?) -> Unit,
    amount: String,
    customPaymentMethods: List<CustomPaymentMethod> = emptyList(),
    selectedCustomCardName: String? = null,
    onCustomCardNameSelected: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(modifier = modifier) {
        SectionLabel(text = strings.paymentMethod)

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PaymentMethod.values().forEach { paymentMethod ->
                val isAvailable = when (paymentMethod) {
                    PaymentMethod.CASH -> true
                    PaymentMethod.CARD -> true
                    PaymentMethod.BALANCE_CARD -> availableBalanceCards.isNotEmpty()
                    PaymentMethod.GIFT_CARD -> availableGiftCards.isNotEmpty()
                }

                if (isAvailable) {
                    SelectablePillChip(
                        label = when (paymentMethod) {
                            PaymentMethod.CASH -> strings.cash
                            PaymentMethod.CARD -> strings.card
                            PaymentMethod.BALANCE_CARD -> strings.balanceCard
                            PaymentMethod.GIFT_CARD -> strings.giftCard
                        },
                        emoji = when (paymentMethod) {
                            PaymentMethod.CASH -> "💵"
                            PaymentMethod.CARD -> "💳"
                            PaymentMethod.BALANCE_CARD -> "🎫"
                            PaymentMethod.GIFT_CARD -> "🎁"
                        },
                        selected = paymentMethod == selectedPaymentMethod,
                        onClick = { onPaymentMethodSelected(paymentMethod) }
                    )
                }
            }
        }

        // 카드 선택 시 커스텀 카드 드롭다운
        if (selectedPaymentMethod == PaymentMethod.CARD && customPaymentMethods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            CustomCardSelectionDropdown(
                customPaymentMethods = customPaymentMethods,
                selectedCardName = selectedCustomCardName,
                onCardNameSelected = onCustomCardNameSelected
            )
        }

        // 잔액권 선택 및 안내
        if (selectedPaymentMethod == PaymentMethod.BALANCE_CARD && availableBalanceCards.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            CardSelectionDropdown(
                cards = availableBalanceCards,
                selectedCard = selectedBalanceCard,
                onCardSelected = onBalanceCardSelected,
                label = strings.selectBalanceCardLabel
            )

            if (selectedBalanceCard != null && amount.isNotBlank()) {
                BalanceCardUsageInfo(
                    balanceCard = selectedBalanceCard,
                    expenseAmount = amount
                )
            }
        }

        // 상품권 선택 및 안내
        if (selectedPaymentMethod == PaymentMethod.GIFT_CARD && availableGiftCards.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            CardSelectionDropdown(
                cards = availableGiftCards,
                selectedCard = selectedGiftCard,
                onCardSelected = onGiftCardSelected,
                label = strings.selectGiftCardLabel
            )

            if (selectedGiftCard != null && amount.isNotBlank()) {
                GiftCardUsageInfo(
                    giftCard = selectedGiftCard,
                    expenseAmount = amount
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    transactionType: TransactionType,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = { },
            readOnly = true,
            label = { Text(strings.category, color = MaterialTheme.colorScheme.onSurface) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .let { modifier ->
                    focusRequester?.let { modifier.focusRequester(it) } ?: modifier
                },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (transactionType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                focusedLabelColor = if (transactionType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> CardSelectionDropdown(
    cards: List<T>,
    selectedCard: T?,
    onCardSelected: (T?) -> Unit,
    label: String
) where T : Any {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = when (selectedCard) {
                is BalanceCard -> "${selectedCard.name} (${selectedCard.currentBalance.toInt()}${strings.currencySymbol})"
                is GiftCard -> "${selectedCard.name} (${selectedCard.remainingAmount.toInt()}${strings.currencySymbol})"
                else -> ""
            },
            onValueChange = { },
            readOnly = true,
            label = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            cards.forEach { card ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = when (card) {
                                is BalanceCard -> "${card.name} (${card.currentBalance.toInt()}${strings.currencySymbol})"
                                is GiftCard -> "${card.name} (${card.remainingAmount.toInt()}${strings.currencySymbol})"
                                else -> card.toString()
                            },
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onCardSelected(card)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BalanceCardUsageInfo(
    balanceCard: BalanceCard,
    expenseAmount: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val amount = expenseAmount.replace(",", "").toDoubleOrNull()
    if (amount != null && amount > 0) {
        val balanceCardAmount = balanceCard.currentBalance

        Spacer(modifier = Modifier.height(8.dp))

        val infoText = when {
            balanceCardAmount >= amount -> {
                val remaining = balanceCardAmount - amount
                strings.balanceCardFullUsage(amount.toInt(), remaining.toInt())
            }
            else -> {
                val cashNeeded = amount - balanceCardAmount
                strings.balanceCardPartialUsage(balanceCardAmount.toInt(), cashNeeded.toInt())
            }
        }

        Text(
            text = "💡 $infoText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun GiftCardUsageInfo(
    giftCard: GiftCard,
    expenseAmount: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val amount = expenseAmount.replace(",", "").toDoubleOrNull()
    if (amount != null && amount > 0) {
        val giftCardAmount = giftCard.remainingAmount

        Spacer(modifier = Modifier.height(8.dp))

        val infoText = when {
            giftCardAmount > amount -> {
                val refund = giftCardAmount - amount
                strings.giftCardRefundUsage(amount.toInt(), refund.toInt())
            }
            giftCardAmount < amount -> {
                val cashNeeded = amount - giftCardAmount
                strings.giftCardPartialUsage(giftCardAmount.toInt(), cashNeeded.toInt())
            }
            else -> {
                strings.giftCardFullUsage(amount.toInt())
            }
        }

        Text(
            text = "💡 $infoText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun SettlementSection(
    isSettlement: Boolean,
    onSettlementChange: (Boolean) -> Unit,
    settlementAmount: TextFieldValue,
    onSettlementAmountChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.dutchPaySettlement,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Switch(
                checked = isSettlement,
                onCheckedChange = onSettlementChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = com.woojin.paymanagement.theme.BrandColor.mint,
                    checkedBorderColor = com.woojin.paymanagement.theme.BrandColor.mint,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }

        AnimatedVisibility(
            visible = isSettlement,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = settlementAmount,
                    onValueChange = onSettlementAmountChange,
                    label = { Text(strings.settlementAmountLabel, color = MaterialTheme.colorScheme.onSurface) },
                    suffix = { Text(strings.currencySymbol, color = MaterialTheme.colorScheme.onSurface) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = FilledInputShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.woojin.paymanagement.theme.BrandColor.mint,
                        focusedLabelColor = com.woojin.paymanagement.theme.BrandColor.mint,
                        cursorColor = com.woojin.paymanagement.theme.BrandColor.mint
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = strings.settlementDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomCardSelectionDropdown(
    customPaymentMethods: List<CustomPaymentMethod>,
    selectedCardName: String?,
    onCardNameSelected: (String?) -> Unit
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedCardName ?: customPaymentMethods.firstOrNull()?.name ?: "",
            onValueChange = { },
            readOnly = true,
            label = { Text(strings.selectCard, color = MaterialTheme.colorScheme.onSurface) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            // 커스텀 카드 목록
            customPaymentMethods.forEach { method ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = method.name,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onCardNameSelected(method.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChipGrid(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    transactionType: TransactionType,
    uiState: AddTransactionUiState,
    selectedSubCategory: String = "",
    onSubCategorySelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    // 선택된 상위 카테고리의 소분류 목록
    val subCategories = uiState.availableCategories
        .filter { it.parentId != null }
        .let { subs ->
            val parentId = uiState.availableCategories
                .firstOrNull { it.name == selectedCategory && it.parentId == null }?.id
            if (parentId != null) subs.filter { it.parentId == parentId } else emptyList()
        }

    Column(modifier = modifier) {
        SectionLabel(text = strings.category)

        Spacer(modifier = Modifier.height(12.dp))

        // 한 줄에 5개씩 아이콘 타일 + 이름 형태로 표시
        val columnCount = 5
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            categories.chunked(columnCount).forEach { rowCategories ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowCategories.forEach { category ->
                        val isSelected = category == selectedCategory
                        // 선택 색상은 기존 규칙 유지: 거래 유형별 연한 배경 + 진한 테두리
                        val backgroundColor = when {
                            isSelected && transactionType == TransactionType.INCOME -> Color(0xFFE3F2FD)
                            isSelected && transactionType == TransactionType.EXPENSE -> Color(0xFFFFEBEE)
                            isSelected && transactionType == TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.lightBackground
                            isSelected && transactionType == TransactionType.INVESTMENT -> com.woojin.paymanagement.theme.InvestmentColor.lightBackground
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val borderColor = when {
                            isSelected && transactionType == TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                            isSelected && transactionType == TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                            isSelected && transactionType == TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                            isSelected && transactionType == TransactionType.INVESTMENT -> com.woojin.paymanagement.theme.InvestmentColor.color
                            else -> Color.Transparent
                        }
                        val categoryEmoji = getCategoryEmoji(category, uiState)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCategorySelected(category) }
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(color = backgroundColor, shape = RoundedCornerShape(18.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(18.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (categoryEmoji.isNotBlank()) {
                                    Text(text = categoryEmoji, fontSize = 24.sp)
                                } else {
                                    // 이모지가 없는 카테고리는 첫 글자로 표시
                                    Text(
                                        text = category.take(1),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) borderColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                    // 마지막 줄의 빈 칸 채우기 (정렬 유지)
                    repeat(columnCount - rowCategories.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 소분류 칩 (선택된 카테고리에 소분류가 있을 때만 표시)
        if (subCategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "소분류",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subCategories.forEach { sub ->
                    val isSubSelected = sub.name == selectedSubCategory
                    val subBackgroundColor = when {
                        isSubSelected && transactionType == TransactionType.INCOME -> Color(0xFFE3F2FD)
                        isSubSelected && transactionType == TransactionType.EXPENSE -> Color(0xFFFFEBEE)
                        isSubSelected && transactionType == TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.lightBackground
                        isSubSelected && transactionType == TransactionType.INVESTMENT -> com.woojin.paymanagement.theme.InvestmentColor.lightBackground
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val subBorderColor = when {
                        isSubSelected && transactionType == TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                        isSubSelected && transactionType == TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                        isSubSelected && transactionType == TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                        isSubSelected && transactionType == TransactionType.INVESTMENT -> com.woojin.paymanagement.theme.InvestmentColor.color
                        else -> Color.Transparent
                    }
                    Row(
                        modifier = Modifier
                            .border(
                                width = if (isSubSelected) 2.dp else 0.dp,
                                color = subBorderColor,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .background(color = subBackgroundColor, shape = RoundedCornerShape(20.dp))
                            .clickable { onSubCategorySelected(if (isSubSelected) "" else sub.name) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (sub.emoji.isNotBlank()) {
                            Text(text = sub.emoji, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSubSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}