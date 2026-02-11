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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.BalanceCard
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

    Column(modifier = modifier) {
        Text(
            text = strings.transactionType,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TransactionType.values().forEach { type ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = (type == selectedType),
                            onClick = { onTypeSelected(type) },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (type == selectedType),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = if (type == TransactionType.INCOME) strings.income else strings.expense,
                        modifier = Modifier.padding(start = 8.dp),
                        color = if (type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

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
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(modifier = modifier) {
        Text(
            text = strings.incomeType,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.selectableGroup()) {
            IncomeType.values().forEach { incomeType ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (incomeType == selectedIncomeType),
                            onClick = { onIncomeTypeSelected(incomeType) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (incomeType == selectedIncomeType),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = when (incomeType) {
                            IncomeType.CASH -> strings.cash
                            IncomeType.BALANCE_CARD -> strings.balanceCard
                            IncomeType.GIFT_CARD -> strings.giftCard
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 잔액권 선택 시
        if (selectedIncomeType == IncomeType.BALANCE_CARD) {
            Spacer(modifier = Modifier.height(8.dp))

            // 기존 잔액권이 있을 때만 선택 옵션 표시
            if (availableBalanceCards.isNotEmpty()) {
                Column(modifier = Modifier.selectableGroup()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = !isChargingExistingBalanceCard,
                                onClick = { onChargingModeChanged(false) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isChargingExistingBalanceCard,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = strings.newBalanceCard,
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isChargingExistingBalanceCard,
                                onClick = { onChargingModeChanged(true) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isChargingExistingBalanceCard,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = strings.chargeExistingBalanceCard,
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(modifier = modifier) {
        Text(
            text = strings.paymentMethod,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.selectableGroup()) {
            PaymentMethod.values().forEach { paymentMethod ->
                val isAvailable = when (paymentMethod) {
                    PaymentMethod.CASH -> true
                    PaymentMethod.CARD -> true
                    PaymentMethod.BALANCE_CARD -> availableBalanceCards.isNotEmpty()
                    PaymentMethod.GIFT_CARD -> availableGiftCards.isNotEmpty()
                }

                if (isAvailable) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (paymentMethod == selectedPaymentMethod),
                                onClick = { onPaymentMethodSelected(paymentMethod) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (paymentMethod == selectedPaymentMethod),
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = when (paymentMethod) {
                                PaymentMethod.CASH -> strings.cash
                                PaymentMethod.CARD -> strings.card
                                PaymentMethod.BALANCE_CARD -> strings.balanceCard
                                PaymentMethod.GIFT_CARD -> strings.giftCard
                            },
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
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
    settlementAmount: String,
    onSettlementAmountChange: (String) -> Unit,
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Switch(
                checked = isSettlement,
                onCheckedChange = onSettlementChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChipGrid(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    transactionType: TransactionType,
    uiState: AddTransactionUiState,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(modifier = modifier) {
        Text(
            text = strings.category,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = category == selectedCategory
                val backgroundColor = when {
                    isSelected && transactionType == TransactionType.INCOME -> Color(0xFFE3F2FD) // 연한 파랑
                    isSelected && transactionType == TransactionType.EXPENSE -> Color(0xFFFFEBEE) // 연한 빨강
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val borderColor = when {
                    isSelected && transactionType == TransactionType.INCOME -> MaterialTheme.colorScheme.primary // 파랑
                    isSelected && transactionType == TransactionType.EXPENSE -> MaterialTheme.colorScheme.error // 빨강
                    else -> Color.Transparent
                }
                val textColor = when {
                    isSelected -> Color.Black  // 선택된 경우 항상 검은색 (배경이 밝은 색이므로)
                    else -> MaterialTheme.colorScheme.onSurface  // 선택되지 않은 경우 테마 색상
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
                        .clickable { onCategorySelected(category) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = getCategoryEmoji(category, uiState),
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
    }
}