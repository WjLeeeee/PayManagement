package com.woojin.paymanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.GiftCardUtils
import com.woojin.paymanagement.data.BalanceCardUtils
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

@Composable
fun AddTransactionScreen(
    selectedDate: LocalDate? = null,
    editTransaction: Transaction? = null,
    availableBalanceCards: List<BalanceCard> = emptyList(),
    availableGiftCards: List<GiftCard> = emptyList(),
    onSave: (List<Transaction>) -> Unit, // 여러 거래를 처리할 수 있도록 변경
    onCancel: () -> Unit
) {
    var amount by remember {
        val initialAmount = editTransaction?.amount?.toLong()?.toString() ?: ""
        mutableStateOf(initialAmount)
    }
    var selectedType by remember { mutableStateOf(editTransaction?.type ?: TransactionType.EXPENSE) }
    var selectedIncomeType by remember { mutableStateOf(editTransaction?.incomeType ?: IncomeType.CASH) }
    var selectedPaymentMethod by remember { mutableStateOf(editTransaction?.paymentMethod ?: PaymentMethod.CASH) }
    var selectedBalanceCard by remember { mutableStateOf<BalanceCard?>(null) }
    var selectedGiftCard by remember { mutableStateOf<GiftCard?>(null) }
    var cardName by remember { mutableStateOf(editTransaction?.cardName ?: "") }
    var category by remember { mutableStateOf(editTransaction?.category ?: "") }
    var memo by remember { mutableStateOf(editTransaction?.memo ?: "") }
    var date by remember {
        mutableStateOf(editTransaction?.date ?: selectedDate ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
    }
    
    val isEditMode = editTransaction != null
    
    val categories = when (selectedType) {
        TransactionType.INCOME -> listOf("급여", "식비", "중고거래", "K-패스 환급", "부수입", "투자수익", "기타수입")
        TransactionType.EXPENSE -> listOf("식비", "교통비", "생활용품", "쇼핑", "의료비", "교육비", "적금", "투자", "기타지출")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (isEditMode) "거래 편집" else "거래 추가",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Transaction Type Selection
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
                            onClick = { selectedType = type },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (type == selectedType),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = if (type == TransactionType.INCOME) Color.Gray else Color.Gray
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

        // Income Type Selection (only for income transactions)
        if (selectedType == TransactionType.INCOME) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "수입 유형",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.selectableGroup()
            ) {
                IncomeType.values().forEach { incomeType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (incomeType == selectedIncomeType),
                                onClick = { selectedIncomeType = incomeType },
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

            // Card name input for BALANCE_CARD and GIFT_CARD
            if (selectedIncomeType == IncomeType.BALANCE_CARD || selectedIncomeType == IncomeType.GIFT_CARD) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
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

        // Payment Method Selection (only for expense transactions)
        if (selectedType == TransactionType.EXPENSE) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "결제 수단",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.selectableGroup()
            ) {
                PaymentMethod.values().forEach { paymentMethod ->
                    // 해당 유형의 카드가 있거나 현금인 경우에만 표시
                    val isAvailable = when (paymentMethod) {
                        PaymentMethod.CASH -> true
                        PaymentMethod.BALANCE_CARD -> availableBalanceCards.isNotEmpty()
                        PaymentMethod.GIFT_CARD -> availableGiftCards.isNotEmpty()
                    }

                    if (isAvailable) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (paymentMethod == selectedPaymentMethod),
                                    onClick = { selectedPaymentMethod = paymentMethod },
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

            // Card selection for BALANCE_CARD and GIFT_CARD
            if (selectedPaymentMethod == PaymentMethod.BALANCE_CARD && availableBalanceCards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                CardSelectionDropdown(
                    cards = availableBalanceCards,
                    selectedCard = selectedBalanceCard,
                    onCardSelected = { selectedBalanceCard = it },
                    label = "잔액권 선택"
                )

                // 잔액권 사용 안내
                if (selectedBalanceCard != null && amount.isNotBlank()) {
                    val expenseAmount = amount.toDoubleOrNull() ?: 0.0
                    val balanceCardAmount = selectedBalanceCard!!.currentBalance

                    Spacer(modifier = Modifier.height(8.dp))

                    val infoText = when {
                        balanceCardAmount >= expenseAmount -> {
                            val remaining = balanceCardAmount - expenseAmount
                            "잔액권 ${expenseAmount.toInt()}원 사용, 잔액 ${remaining.toInt()}원"
                        }
                        else -> {
                            val cashNeeded = expenseAmount - balanceCardAmount
                            "잔액권 ${balanceCardAmount.toInt()}원 + 현금 ${cashNeeded.toInt()}원 지출"
                        }
                    }

                    Text(
                        text = "💡 $infoText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            if (selectedPaymentMethod == PaymentMethod.GIFT_CARD && availableGiftCards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                CardSelectionDropdown(
                    cards = availableGiftCards,
                    selectedCard = selectedGiftCard,
                    onCardSelected = { selectedGiftCard = it },
                    label = "상품권 선택"
                )

                // 상품권 사용 안내
                if (selectedGiftCard != null && amount.isNotBlank()) {
                    val expenseAmount = amount.toDoubleOrNull() ?: 0.0
                    val giftCardAmount = selectedGiftCard!!.remainingAmount

                    Spacer(modifier = Modifier.height(8.dp))

                    val infoText = when {
                        giftCardAmount > expenseAmount -> {
                            val refund = giftCardAmount - expenseAmount
                            "상품권 ${expenseAmount.toInt()}원 사용, ${refund.toInt()}원 현금 환급"
                        }
                        giftCardAmount < expenseAmount -> {
                            val cashNeeded = expenseAmount - giftCardAmount
                            "상품권 ${giftCardAmount.toInt()}원 + 현금 ${cashNeeded.toInt()}원 지출"
                        }
                        else -> {
                            "상품권 ${expenseAmount.toInt()}원 전액 사용"
                        }
                    }

                    Text(
                        text = "💡 $infoText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Amount Input
        OutlinedTextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d+\\.?\\d*$"))) {
                    amount = newValue
                }
            },
            label = { Text("금액", color = Color.Black) },
            suffix = { Text("원", color = Color.Black) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (selectedType == TransactionType.INCOME) Color.Blue else Color.Red,
                focusedLabelColor = if (selectedType == TransactionType.INCOME) Color.Blue else Color.Red,
                unfocusedTextColor = Color.Black,
                focusedTextColor = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category Selection
        CategoryDropdown(
            categories = categories,
            selectedCategory = category,
            onCategorySelected = { category = it },
            transactionType = selectedType
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date Input
        OutlinedTextField(
            value = "${date.year}-${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}",
            onValueChange = { },
            label = { Text("날짜", color = Color.Black) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = Color.Black,
                focusedTextColor = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Memo Input
        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it },
            label = { Text("메모 (선택사항)", color = Color.Black) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = Color.Black,
                focusedTextColor = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Save/Cancel Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("취소")
            }
            
            Button(
                onClick = {
                    val isValidInput = amount.isNotBlank() && category.isNotBlank() &&
                            // 수입일 때: 현금이거나 카드 이름이 입력됨
                            (selectedType == TransactionType.EXPENSE ||
                             selectedIncomeType == IncomeType.CASH ||
                             cardName.isNotBlank()) &&
                            // 지출일 때: 현금이거나 카드가 선택됨
                            (selectedType == TransactionType.INCOME ||
                             selectedPaymentMethod == PaymentMethod.CASH ||
                             (selectedPaymentMethod == PaymentMethod.BALANCE_CARD && selectedBalanceCard != null) ||
                             (selectedPaymentMethod == PaymentMethod.GIFT_CARD && selectedGiftCard != null))

                    if (isValidInput) {
                        val expenseAmount = amount.toDoubleOrNull() ?: 0.0

                        // 잔액권 지출 시 특별 처리
                        if (selectedType == TransactionType.EXPENSE && selectedPaymentMethod == PaymentMethod.BALANCE_CARD && selectedBalanceCard != null) {
                            val baseTransaction = Transaction(
                                id = editTransaction?.id ?: Random.nextLong().toString(),
                                amount = expenseAmount,
                                type = selectedType,
                                category = category,
                                memo = memo,
                                date = date,
                                paymentMethod = selectedPaymentMethod,
                                balanceCardId = selectedBalanceCard!!.id,
                                cardName = selectedBalanceCard!!.name
                            )

                            val result = BalanceCardUtils.processBalanceCardExpense(
                                balanceCard = selectedBalanceCard!!,
                                expenseAmount = expenseAmount,
                                baseTransaction = baseTransaction
                            )

                            onSave(result.transactions)
                        }
                        // 상품권 지출 시 특별 처리
                        else if (selectedType == TransactionType.EXPENSE && selectedPaymentMethod == PaymentMethod.GIFT_CARD && selectedGiftCard != null) {
                            val baseTransaction = Transaction(
                                id = editTransaction?.id ?: Random.nextLong().toString(),
                                amount = expenseAmount,
                                type = selectedType,
                                category = category,
                                memo = memo,
                                date = date,
                                paymentMethod = selectedPaymentMethod,
                                giftCardId = selectedGiftCard!!.id,
                                cardName = selectedGiftCard!!.name
                            )

                            val result = GiftCardUtils.processGiftCardExpense(
                                giftCard = selectedGiftCard!!,
                                expenseAmount = expenseAmount,
                                baseTransaction = baseTransaction
                            )

                            onSave(result.transactions)
                        } else {
                            // 일반 거래 처리
                            val transaction = Transaction(
                                id = editTransaction?.id ?: Random.nextLong().toString(),
                                amount = expenseAmount,
                                type = selectedType,
                                category = category,
                                memo = memo,
                                date = date,
                                incomeType = if (selectedType == TransactionType.INCOME) selectedIncomeType else null,
                                paymentMethod = if (selectedType == TransactionType.EXPENSE) selectedPaymentMethod else null,
                                balanceCardId = when {
                                    selectedType == TransactionType.INCOME && selectedIncomeType == IncomeType.BALANCE_CARD -> Random.nextLong().toString()
                                    selectedType == TransactionType.EXPENSE && selectedPaymentMethod == PaymentMethod.BALANCE_CARD -> selectedBalanceCard?.id
                                    else -> null
                                },
                                giftCardId = when {
                                    selectedType == TransactionType.INCOME && selectedIncomeType == IncomeType.GIFT_CARD -> Random.nextLong().toString()
                                    else -> null
                                },
                                cardName = when {
                                    selectedType == TransactionType.INCOME && (selectedIncomeType == IncomeType.BALANCE_CARD || selectedIncomeType == IncomeType.GIFT_CARD) -> cardName
                                    selectedType == TransactionType.EXPENSE && selectedPaymentMethod == PaymentMethod.BALANCE_CARD -> selectedBalanceCard?.name
                                    else -> null
                                }
                            )
                            onSave(listOf(transaction))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = amount.isNotBlank() && category.isNotBlank() &&
                          // 수입 유효성 검사
                          (selectedType == TransactionType.EXPENSE ||
                           selectedIncomeType == IncomeType.CASH ||
                           cardName.isNotBlank()) &&
                          // 지출 유효성 검사
                          (selectedType == TransactionType.INCOME ||
                           selectedPaymentMethod == PaymentMethod.CASH ||
                           (selectedPaymentMethod == PaymentMethod.BALANCE_CARD && selectedBalanceCard != null) ||
                           (selectedPaymentMethod == PaymentMethod.GIFT_CARD && selectedGiftCard != null)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray
                )
            ) {
                Text("저장", color = Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    transactionType: TransactionType
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
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