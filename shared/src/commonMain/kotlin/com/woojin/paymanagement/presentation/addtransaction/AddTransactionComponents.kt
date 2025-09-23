package com.woojin.paymanagement.presentation.addtransaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.TransactionType

@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "거래 유형",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
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
                            selectedColor = Color.Gray
                        )
                    )
                    Text(
                        text = if (type == TransactionType.INCOME) "수입" else "지출",
                        modifier = Modifier.padding(start = 8.dp),
                        color = if (type == TransactionType.INCOME) Color.Blue else Color.Red,
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "수입 유형",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
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
                            selectedColor = Color.Gray
                        )
                    )
                    Text(
                        text = when (incomeType) {
                            IncomeType.CASH -> "현금"
                            IncomeType.BALANCE_CARD -> "잔액권"
                            IncomeType.GIFT_CARD -> "상품권"
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (selectedIncomeType == IncomeType.BALANCE_CARD || selectedIncomeType == IncomeType.GIFT_CARD) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = cardName,
                onValueChange = onCardNameChanged,
                label = {
                    Text(
                        text = when (selectedIncomeType) {
                            IncomeType.BALANCE_CARD -> "잔액권 이름 (예: 편의점 상품권)"
                            IncomeType.GIFT_CARD -> "상품권 이름 (예: 신세계 상품권)"
                            else -> ""
                        },
                        color = Color.Black
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.Gray,
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Black
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
    Column(modifier = modifier) {
        Text(
            text = "결제 수단",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
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
                                selectedColor = Color.Gray
                            )
                        )
                        Text(
                            text = when (paymentMethod) {
                                PaymentMethod.CASH -> "현금"
                                PaymentMethod.CARD -> "카드"
                                PaymentMethod.BALANCE_CARD -> "잔액권"
                                PaymentMethod.GIFT_CARD -> "상품권"
                            },
                            modifier = Modifier.padding(start = 8.dp),
                            color = Color.Black,
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
                label = "잔액권 선택"
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
                label = "상품권 선택"
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
    modifier: Modifier = Modifier
) {
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
            label = { Text("카테고리", color = Color.Black) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (transactionType == TransactionType.INCOME) Color.Blue else Color.Red,
                focusedLabelColor = if (transactionType == TransactionType.INCOME) Color.Blue else Color.Red,
                unfocusedTextColor = Color.Black,
                focusedTextColor = Color.Black
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category, color = Color.Black) },
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
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = when (selectedCard) {
                is BalanceCard -> "${selectedCard.name} (${selectedCard.currentBalance.toInt()}원)"
                is GiftCard -> "${selectedCard.name} (${selectedCard.remainingAmount.toInt()}원)"
                else -> ""
            },
            onValueChange = { },
            readOnly = true,
            label = { Text(label, color = Color.Black) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                focusedLabelColor = Color.Gray,
                unfocusedTextColor = Color.Black,
                focusedTextColor = Color.Black
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
                                is BalanceCard -> "${card.name} (${card.currentBalance.toInt()}원)"
                                is GiftCard -> "${card.name} (${card.remainingAmount.toInt()}원)"
                                else -> card.toString()
                            },
                            color = Color.Black
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
    val amount = expenseAmount.replace(",", "").toDoubleOrNull()
    if (amount != null && amount > 0) {
        val balanceCardAmount = balanceCard.currentBalance

        Spacer(modifier = Modifier.height(8.dp))

        val infoText = when {
            balanceCardAmount >= amount -> {
                val remaining = balanceCardAmount - amount
                "잔액권 ${amount.toInt()}원 사용, 잔액 ${remaining.toInt()}원"
            }
            else -> {
                val cashNeeded = amount - balanceCardAmount
                "잔액권 ${balanceCardAmount.toInt()}원 + 현금 ${cashNeeded.toInt()}원 지출"
            }
        }

        Text(
            text = "💡 $infoText",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
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
    val amount = expenseAmount.replace(",", "").toDoubleOrNull()
    if (amount != null && amount > 0) {
        val giftCardAmount = giftCard.remainingAmount

        Spacer(modifier = Modifier.height(8.dp))

        val infoText = when {
            giftCardAmount > amount -> {
                val refund = giftCardAmount - amount
                "상품권 ${amount.toInt()}원 사용, ${refund.toInt()}원 현금 환급"
            }
            giftCardAmount < amount -> {
                val cashNeeded = amount - giftCardAmount
                "상품권 ${giftCardAmount.toInt()}원 + 현금 ${cashNeeded.toInt()}원 지출"
            }
            else -> {
                "상품권 ${amount.toInt()}원 전액 사용"
            }
        }

        Text(
            text = "💡 $infoText",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun SettlementSection(
    isSettlement: Boolean,
    onSettlementChange: (Boolean) -> Unit,
    actualAmount: String,
    onActualAmountChange: (String) -> Unit,
    splitCount: String,
    onSplitCountChange: (String) -> Unit,
    settlementAmount: String,
    onSettlementAmountChange: (String) -> Unit,
    myAmount: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "더치페이/정산",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Switch(
                checked = isSettlement,
                onCheckedChange = onSettlementChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Gray,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.LightGray
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
                    value = actualAmount,
                    onValueChange = onActualAmountChange,
                    label = { Text("실제 결제 금액", color = Color.Black) },
                    suffix = { Text("원", color = Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Gray,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = splitCount,
                        onValueChange = onSplitCountChange,
                        label = { Text("분할 인원", color = Color.Black) },
                        suffix = { Text("명", color = Color.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            focusedLabelColor = Color.Gray,
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black
                        )
                    )

                    OutlinedTextField(
                        value = settlementAmount,
                        onValueChange = onSettlementAmountChange,
                        label = { Text("정산받을 금액", color = Color.Black) },
                        suffix = { Text("원", color = Color.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            focusedLabelColor = Color.Gray,
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (myAmount.isNotBlank()) {
                    Text(
                        text = "💡 내 부담액: ${myAmount}원",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}