package com.woojin.paymanagement.presentation.addtransaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.utils.BackHandler
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
fun AddTransactionScreen(
    transactions: List<Transaction>,
    selectedDate: LocalDate? = null,
    editTransaction: Transaction? = null,
    parsedTransaction: com.woojin.paymanagement.data.ParsedTransaction? = null,
    viewModel: AddTransactionViewModel,
    onSave: (List<Transaction>) -> Unit,
    onCancel: () -> Unit
) {
    // 시스템 뒤로가기 버튼 처리
    BackHandler(onBack = onCancel)

    val uiState = viewModel.uiState
    val scope = rememberCoroutineScope()
    val categoryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(transactions, selectedDate, editTransaction, parsedTransaction) {
        if (parsedTransaction != null) {
            viewModel.initializeWithParsedTransaction(transactions, parsedTransaction)
        } else {
            viewModel.initialize(transactions, selectedDate, editTransaction)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (uiState.isEditMode) "거래 편집" else "거래 추가",
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
                onCardNameChanged = viewModel::updateCardName
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
                amount = uiState.amount.text
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Amount Input
        OutlinedTextField(
            value = uiState.amount,
            onValueChange = viewModel::updateAmount,
            label = { Text("금액") },
            suffix = { Text("원") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    scope.launch {
                        categoryFocusRequester.requestFocus()
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (uiState.selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                focusedLabelColor = if (uiState.selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        )

        // Settlement Section (for expense only)
        if (uiState.selectedType == TransactionType.EXPENSE) {
            Spacer(modifier = Modifier.height(16.dp))

            SettlementSection(
                isSettlement = uiState.isSettlement,
                onSettlementChange = viewModel::updateSettlement,
                actualAmount = uiState.actualAmount,
                onActualAmountChange = viewModel::updateActualAmount,
                splitCount = uiState.splitCount,
                onSplitCountChange = viewModel::updateSplitCount,
                settlementAmount = uiState.settlementAmount,
                onSettlementAmountChange = viewModel::updateSettlementAmount,
                myAmount = uiState.amount.text
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Selection
        CategoryChipGrid(
            categories = uiState.categories,
            selectedCategory = uiState.category,
            onCategorySelected = viewModel::updateCategory,
            transactionType = uiState.selectedType
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date Input
        OutlinedTextField(
            value = uiState.date?.let {
                "${it.year}-${it.monthNumber.toString().padStart(2, '0')}-${it.dayOfMonth.toString().padStart(2, '0')}"
            } ?: "",
            onValueChange = { },
            label = { Text("날짜") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Memo Input
        OutlinedTextField(
            value = uiState.memo,
            onValueChange = viewModel::updateMemo,
            label = { Text("메모 (선택사항)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(32.dp))

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
                    text = "취소",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        val savedTransactions = viewModel.saveTransaction()
                        if (savedTransactions.isNotEmpty()) {
                            onSave(savedTransactions)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                enabled = uiState.saveEnabled && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.selectedType == TransactionType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = if (uiState.isLoading) "저장 중..." else "저장",
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