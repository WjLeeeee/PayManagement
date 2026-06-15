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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
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
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.BackHandler
import com.woojin.paymanagement.utils.formatWithCommas
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
                .padding(16.dp)
        ) {
        Text(
            text = if (uiState.isEditMode) strings.editTransaction else strings.addTransaction,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        // Amount Input
        OutlinedTextField(
            value = uiState.amount,
            onValueChange = viewModel::updateAmount,
            label = { Text(strings.transactionAmount) },
            suffix = { Text(strings.currencySymbol) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = when (uiState.selectedType) {
                    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                    TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                    TransactionType.INVESTMENT -> com.woojin.paymanagement.theme.InvestmentColor.color
                },
                focusedLabelColor = when (uiState.selectedType) {
                    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                    TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                    TransactionType.INVESTMENT -> com.woojin.paymanagement.theme.InvestmentColor.color
                }
            )
        )

        // 금액 퀵 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickAmountColor = when (uiState.selectedType) {
                TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                TransactionType.INVESTMENT -> com.woojin.paymanagement.theme.InvestmentColor.color
            }
            listOf(1_000L, 5_000L, 10_000L, 50_000L).forEach { quickAmount ->
                OutlinedButton(
                    onClick = { viewModel.addQuickAmount(quickAmount) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text(
                        text = "+${formatWithCommas(quickAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = quickAmountColor
                    )
                }
            }
        }

        // Settlement Section (for expense only)
        if (uiState.selectedType == TransactionType.EXPENSE) {
            Spacer(modifier = Modifier.height(16.dp))

            SettlementSection(
                isSettlement = uiState.isSettlement,
                onSettlementChange = viewModel::updateSettlement,
                settlementAmount = uiState.settlementAmount,
                onSettlementAmountChange = viewModel::updateSettlementAmount
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Selection
        CategoryChipGrid(
            categories = uiState.categories,
            selectedCategory = uiState.category,
            onCategorySelected = viewModel::updateCategory,
            transactionType = uiState.selectedType,
            uiState = uiState
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date Input
        OutlinedTextField(
            value = uiState.date?.let {
                "${it.year}-${it.monthNumber.toString().padStart(2, '0')}-${it.dayOfMonth.toString().padStart(2, '0')}"
            } ?: "",
            onValueChange = { },
            label = { Text(strings.dateLabel) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Merchant Input (지출일 때만 표시)
        if (uiState.selectedType == TransactionType.EXPENSE) {
            val suggestionRequester = remember { BringIntoViewRequester() }
            LaunchedEffect(uiState.merchantSuggestions) {
                if (uiState.merchantSuggestions.isNotEmpty()) {
                    suggestionRequester.bringIntoView()
                }
            }
            OutlinedTextField(
                value = uiState.merchant,
                onValueChange = viewModel::updateMerchant,
                label = { Text(strings.merchantLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (uiState.merchantSuggestions.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(suggestionRequester),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.merchantSuggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { viewModel.selectMerchantSuggestion(suggestion) },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Memo Input
        OutlinedTextField(
            value = uiState.memo,
            onValueChange = viewModel::updateMemo,
            label = { Text(strings.memoOptional) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 공유방 참여 중일 때 저장 대상 선택
        if (uiState.isInSharedRoom) {
            SaveTargetSelector(
                selected = uiState.saveTarget,
                onSelect = viewModel::updateSaveTarget
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Save/Cancel Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = strings.cancel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

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
                    .weight(1f)
                    .height(40.dp),
                enabled = uiState.saveEnabled && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (uiState.selectedType) {
                        TransactionType.INCOME -> Color(0xFF4CAF50)
                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                        TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                        TransactionType.INVESTMENT -> com.woojin.paymanagement.theme.InvestmentColor.color
                    },
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = if (uiState.isLoading) strings.savingTransaction else strings.save,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

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
        Text(
            text = "저장 대상",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (target, label) ->
                val isSelected = selected == target
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelect(target) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}