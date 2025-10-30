package com.woojin.paymanagement.presentation.addtransaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.woojin.paymanagement.data.BalanceCard
import com.woojin.paymanagement.data.BalanceCardUtils
import com.woojin.paymanagement.data.GiftCard
import com.woojin.paymanagement.data.GiftCardUtils
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.ParsedTransaction
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.usecase.GetAvailableBalanceCardsUseCase
import com.woojin.paymanagement.domain.usecase.GetAvailableGiftCardsUseCase
import com.woojin.paymanagement.domain.usecase.SaveMultipleTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.SaveTransactionUseCase
import com.woojin.paymanagement.domain.usecase.UpdateTransactionUseCase
import com.woojin.paymanagement.domain.usecase.GetCategoriesUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.woojin.paymanagement.utils.formatWithCommas
import com.woojin.paymanagement.utils.parseAmountToDouble
import com.woojin.paymanagement.utils.removeCommas
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

class AddTransactionViewModel(
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val saveMultipleTransactionsUseCase: SaveMultipleTransactionsUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val getAvailableBalanceCardsUseCase: GetAvailableBalanceCardsUseCase,
    private val getAvailableGiftCardsUseCase: GetAvailableGiftCardsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {
    var uiState by mutableStateOf(AddTransactionUiState())
        private set

    private var categoriesJob: Job? = null

    init {
        // 타입 변경 시 카테고리 로드
        loadCategories()
    }

    private fun loadCategories() {
        // 이전 collect Job 취소
        categoriesJob?.cancel()

        // 새로운 collect Job 시작
        categoriesJob = viewModelScope.launch {
            getCategoriesUseCase(uiState.selectedType).collectLatest { categories ->
                uiState = uiState.copy(
                    availableCategories = categories
                )
            }
        }
    }


    private fun generateUniqueId(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val random = Random.nextInt(1000, 9999)
        return "${timestamp}_$random"
    }


    fun reset() {
        uiState = AddTransactionUiState()
    }

    fun initialize(
        transactions: List<Transaction>,
        selectedDate: LocalDate?,
        editTransaction: Transaction?
    ) {
        // 편집 모드가 아닌 경우 상태 초기화
        if (editTransaction == null) {
            reset()
        }

        val availableBalanceCards = getAvailableBalanceCardsUseCase(transactions)
        val availableGiftCards = getAvailableGiftCardsUseCase(transactions)

        val initialDate = editTransaction?.date
            ?: selectedDate
            ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        if (editTransaction != null) {
            // 편집 모드 초기화
            val initialAmount = editTransaction.amount.toLong().toString()
            uiState = uiState.copy(
                amount = TextFieldValue(
                    text = if (initialAmount.isNotEmpty()) formatWithCommas(initialAmount.toLong()) else "",
                    selection = TextRange(if (initialAmount.isNotEmpty()) formatWithCommas(initialAmount.toLong()).length else 0)
                ),
                selectedType = editTransaction.type,
                selectedIncomeType = editTransaction.incomeType ?: IncomeType.CASH,
                selectedPaymentMethod = editTransaction.paymentMethod ?: PaymentMethod.CASH,
                cardName = editTransaction.cardName ?: "",
                category = editTransaction.category,
                merchant = editTransaction.merchant ?: "",
                memo = editTransaction.memo,
                date = initialDate,
                isSettlement = editTransaction.isSettlement,
                actualAmount = editTransaction.actualAmount?.toLong()?.toString() ?: "",
                settlementAmount = editTransaction.settlementAmount?.toLong()?.toString() ?: "",
                availableBalanceCards = availableBalanceCards,
                availableGiftCards = availableGiftCards,
                isEditMode = true,
                editTransaction = editTransaction
            )
        } else {
            // 새 거래 추가 모드
            uiState = uiState.copy(
                date = initialDate,
                availableBalanceCards = availableBalanceCards,
                availableGiftCards = availableGiftCards,
                isEditMode = false,
                editTransaction = null
            )
        }

        // 최신 카테고리 로드
        loadCategories()

        validateInput()
    }

    fun initializeWithParsedTransaction(
        transactions: List<Transaction>,
        parsedTransaction: ParsedTransaction
    ) {
        reset()

        val availableBalanceCards = getAvailableBalanceCardsUseCase(transactions)
        val availableGiftCards = getAvailableGiftCardsUseCase(transactions)

        val amountText = formatWithCommas(parsedTransaction.amount.toLong())

        uiState = uiState.copy(
            amount = TextFieldValue(
                text = amountText,
                selection = TextRange(amountText.length)
            ),
            selectedType = TransactionType.EXPENSE, // 카드 사용은 지출
            selectedPaymentMethod = PaymentMethod.CARD, // 결제수단은 카드
            memo = parsedTransaction.merchantName, // 가맹점명을 메모에
            date = parsedTransaction.date, // 파싱된 날짜
            availableBalanceCards = availableBalanceCards,
            availableGiftCards = availableGiftCards,
            isEditMode = false,
            editTransaction = null
        )

        // 최신 카테고리 로드
        loadCategories()

        validateInput()
    }

    fun updateAmount(newValue: TextFieldValue) {
        val digitsOnly = removeCommas(newValue.text)

        if (digitsOnly.isEmpty() || digitsOnly.matches(Regex("^\\d+$"))) {
            val formattedAmount = if (digitsOnly.isNotEmpty()) {
                val number = digitsOnly.toLongOrNull() ?: 0L
                formatWithCommas(number)
            } else {
                ""
            }

            // 커서를 항상 텍스트 끝에 위치시켜 자연스러운 숫자 입력 제공
            uiState = uiState.copy(
                amount = TextFieldValue(
                    text = formattedAmount,
                    selection = TextRange(formattedAmount.length)
                )
            )

            // 더치페이 활성화 시 정산받을 금액 자동 계산
            if (uiState.isSettlement && uiState.actualAmount.isNotBlank()) {
                val actual = parseAmountToDouble(uiState.actualAmount)
                val myAmount = digitsOnly.toDoubleOrNull() ?: 0.0
                if (actual > myAmount) {
                    val settlementValue = (actual - myAmount).toLong()
                    uiState = uiState.copy(
                        settlementAmount = formatWithCommas(settlementValue)
                    )
                }
            }

            validateInput()
        }
    }

    fun updateTransactionType(type: TransactionType) {
        if (uiState.selectedType != type) {
            uiState = uiState.copy(
                selectedType = type,
                category = ""
            )
            loadCategories() // 타입 변경 시 카테고리 다시 로드
            validateInput()
        }
    }

    fun updateIncomeType(incomeType: IncomeType) {
        uiState = uiState.copy(selectedIncomeType = incomeType)
        validateInput()
    }

    fun updatePaymentMethod(paymentMethod: PaymentMethod) {
        uiState = uiState.copy(selectedPaymentMethod = paymentMethod)
        validateInput()
    }

    fun updateSelectedBalanceCard(card: BalanceCard?) {
        uiState = uiState.copy(selectedBalanceCard = card)
        validateInput()
    }

    fun updateSelectedGiftCard(card: GiftCard?) {
        uiState = uiState.copy(selectedGiftCard = card)
        validateInput()
    }

    fun updateCardName(name: String) {
        uiState = uiState.copy(cardName = name)
        validateInput()
    }

    fun updateChargingMode(isCharging: Boolean) {
        uiState = uiState.copy(
            isChargingExistingBalanceCard = isCharging,
            // 모드 변경 시 선택/입력 초기화
            selectedBalanceCardForCharge = null,
            cardName = ""
        )
        validateInput()
    }

    fun updateSelectedBalanceCardForCharge(card: BalanceCard?) {
        uiState = uiState.copy(selectedBalanceCardForCharge = card)
        validateInput()
    }

    fun updateCategory(category: String) {
        uiState = uiState.copy(category = category)
        validateInput()
    }

    fun updateMerchant(merchant: String) {
        uiState = uiState.copy(merchant = merchant)
        validateInput()
    }

    fun updateMemo(memo: String) {
        uiState = uiState.copy(memo = memo)
    }

    fun updateSettlement(isSettlement: Boolean) {
        uiState = if (isSettlement) {
            uiState.copy(
                isSettlement = true,
                selectedPaymentMethod = PaymentMethod.CARD
            )
        } else {
            uiState.copy(
                isSettlement = false,
                actualAmount = "",
                splitCount = "",
                settlementAmount = ""
            )
        }
        validateInput()
    }

    fun updateActualAmount(newValue: String) {
        val digitsOnly = removeCommas(newValue)

        if (digitsOnly.isEmpty() || digitsOnly.matches(Regex("^\\d+$"))) {
            val formattedAmount = if (digitsOnly.isNotEmpty()) {
                val number = digitsOnly.toLongOrNull() ?: 0L
                formatWithCommas(number)
            } else {
                ""
            }

            uiState = uiState.copy(actualAmount = formattedAmount)

            val actual = digitsOnly.toDoubleOrNull() ?: 0.0
            val split = uiState.splitCount.toIntOrNull() ?: 0
            if (actual > 0 && split > 0) {
                val myShare = actual / split
                val myShareText = formatWithCommas(myShare.toLong())
                uiState = uiState.copy(
                    amount = TextFieldValue(
                        text = myShareText,
                        selection = TextRange(myShareText.length)
                    ),
                    settlementAmount = formatWithCommas((actual - myShare).toLong())
                )
            }
        }
    }

    fun updateSplitCount(newValue: String) {
        if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
            uiState = uiState.copy(splitCount = newValue)

            val actual = parseAmountToDouble(uiState.actualAmount)
            val split = newValue.toIntOrNull() ?: 0
            if (actual > 0 && split > 0) {
                val myShare = actual / split
                val myShareText = formatWithCommas(myShare.toLong())
                uiState = uiState.copy(
                    amount = TextFieldValue(
                        text = myShareText,
                        selection = TextRange(myShareText.length)
                    ),
                    settlementAmount = formatWithCommas((actual - myShare).toLong())
                )
            }
        }
    }

    fun updateSettlementAmount(newValue: String) {
        if (newValue.isEmpty() || newValue.matches(Regex("^\\d+\\.?\\d*$"))) {
            uiState = uiState.copy(settlementAmount = newValue)

            val actual = uiState.actualAmount.toDoubleOrNull() ?: 0.0
            val settlement = newValue.toDoubleOrNull() ?: 0.0
            if (actual > settlement) {
                val newAmountText = (actual - settlement).toInt().toString()
                uiState = uiState.copy(
                    amount = TextFieldValue(
                        text = newAmountText,
                        selection = TextRange(newAmountText.length)
                    )
                )
            }
        }
    }

    private fun validateInput() {
        val isValidInput = uiState.amount.text.isNotBlank() &&
                          uiState.category.isNotBlank() &&
                          // 지출일 때 사용처 필수
                          (uiState.selectedType == TransactionType.INCOME || uiState.merchant.isNotBlank()) &&
                          // 수입일 때 검증
                          (uiState.selectedType == TransactionType.EXPENSE ||
                           uiState.selectedIncomeType == IncomeType.CASH ||
                           (uiState.selectedIncomeType == IncomeType.BALANCE_CARD && !uiState.isChargingExistingBalanceCard && uiState.cardName.isNotBlank()) ||
                           (uiState.selectedIncomeType == IncomeType.BALANCE_CARD && uiState.isChargingExistingBalanceCard && uiState.selectedBalanceCardForCharge != null) ||
                           (uiState.selectedIncomeType == IncomeType.GIFT_CARD && uiState.cardName.isNotBlank())) &&
                          // 지출일 때: 현금이거나 카드가 선택됨
                          (uiState.selectedType == TransactionType.INCOME ||
                           uiState.selectedPaymentMethod == PaymentMethod.CASH ||
                           uiState.selectedPaymentMethod == PaymentMethod.CARD ||
                           (uiState.selectedPaymentMethod == PaymentMethod.BALANCE_CARD && uiState.selectedBalanceCard != null) ||
                           (uiState.selectedPaymentMethod == PaymentMethod.GIFT_CARD && uiState.selectedGiftCard != null))

        uiState = uiState.copy(
            isValidInput = isValidInput,
            saveEnabled = isValidInput
        )
    }

    suspend fun saveTransaction(): List<Transaction> {
        if (!uiState.isValidInput || uiState.date == null) return emptyList()

        try {
            uiState = uiState.copy(isLoading = true, error = null)

            val expenseAmount = parseAmountToDouble(uiState.amount.text)
            val currentDate = uiState.date!!

            val transactions = when {
                // 잔액권 지출 시 특별 처리
                uiState.selectedType == TransactionType.EXPENSE &&
                uiState.selectedPaymentMethod == PaymentMethod.BALANCE_CARD &&
                uiState.selectedBalanceCard != null -> {

                    val baseTransaction = Transaction(
                        id = if (uiState.isEditMode) uiState.editTransaction?.id ?: generateUniqueId() else generateUniqueId(),
                        amount = expenseAmount,
                        type = uiState.selectedType,
                        category = uiState.category,
                        merchant = uiState.merchant.ifBlank { null },
                        memo = uiState.memo,
                        date = currentDate,
                        paymentMethod = uiState.selectedPaymentMethod,
                        balanceCardId = uiState.selectedBalanceCard!!.id,
                        cardName = uiState.selectedBalanceCard!!.name
                    )

                    val result = BalanceCardUtils.processBalanceCardExpense(
                        balanceCard = uiState.selectedBalanceCard!!,
                        expenseAmount = expenseAmount,
                        baseTransaction = baseTransaction
                    )

                    result.transactions
                }

                // 상품권 지출 시 특별 처리
                uiState.selectedType == TransactionType.EXPENSE &&
                uiState.selectedPaymentMethod == PaymentMethod.GIFT_CARD &&
                uiState.selectedGiftCard != null -> {

                    val baseTransaction = Transaction(
                        id = if (uiState.isEditMode) uiState.editTransaction?.id ?: generateUniqueId() else generateUniqueId(),
                        amount = expenseAmount,
                        type = uiState.selectedType,
                        category = uiState.category,
                        merchant = uiState.merchant.ifBlank { null },
                        memo = uiState.memo,
                        date = currentDate,
                        paymentMethod = uiState.selectedPaymentMethod,
                        giftCardId = uiState.selectedGiftCard!!.id,
                        cardName = uiState.selectedGiftCard!!.name
                    )

                    val result = GiftCardUtils.processGiftCardExpense(
                        giftCard = uiState.selectedGiftCard!!,
                        expenseAmount = expenseAmount,
                        baseTransaction = baseTransaction
                    )

                    result.transactions
                }

                // 일반 거래 처리
                else -> {
                    val transaction = Transaction(
                        id = if (uiState.isEditMode) uiState.editTransaction?.id ?: generateUniqueId() else generateUniqueId(),
                        amount = expenseAmount,
                        type = uiState.selectedType,
                        category = uiState.category,
                        merchant = if (uiState.selectedType == TransactionType.EXPENSE) uiState.merchant.ifBlank { null } else null,
                        memo = uiState.memo,
                        date = currentDate,
                        incomeType = if (uiState.selectedType == TransactionType.INCOME) uiState.selectedIncomeType else null,
                        paymentMethod = if (uiState.selectedType == TransactionType.EXPENSE) uiState.selectedPaymentMethod else null,
                        balanceCardId = when {
                            // 수입 - 잔액권 새로 추가
                            uiState.selectedType == TransactionType.INCOME &&
                            uiState.selectedIncomeType == IncomeType.BALANCE_CARD &&
                            !uiState.isChargingExistingBalanceCard -> generateUniqueId()
                            // 수입 - 기존 잔액권 충전
                            uiState.selectedType == TransactionType.INCOME &&
                            uiState.selectedIncomeType == IncomeType.BALANCE_CARD &&
                            uiState.isChargingExistingBalanceCard -> uiState.selectedBalanceCardForCharge?.id
                            // 지출 - 잔액권 사용
                            uiState.selectedType == TransactionType.EXPENSE &&
                            uiState.selectedPaymentMethod == PaymentMethod.BALANCE_CARD -> uiState.selectedBalanceCard?.id
                            else -> null
                        },
                        giftCardId = when {
                            // 상품권은 항상 새로 추가
                            uiState.selectedType == TransactionType.INCOME &&
                            uiState.selectedIncomeType == IncomeType.GIFT_CARD -> generateUniqueId()
                            else -> null
                        },
                        cardName = when {
                            // 수입 - 잔액권 새로 추가
                            uiState.selectedType == TransactionType.INCOME &&
                            uiState.selectedIncomeType == IncomeType.BALANCE_CARD &&
                            !uiState.isChargingExistingBalanceCard -> uiState.cardName
                            // 수입 - 기존 잔액권 충전
                            uiState.selectedType == TransactionType.INCOME &&
                            uiState.selectedIncomeType == IncomeType.BALANCE_CARD &&
                            uiState.isChargingExistingBalanceCard -> uiState.selectedBalanceCardForCharge?.name
                            // 수입 - 상품권
                            uiState.selectedType == TransactionType.INCOME &&
                            uiState.selectedIncomeType == IncomeType.GIFT_CARD -> uiState.cardName
                            // 지출 - 잔액권 사용
                            uiState.selectedType == TransactionType.EXPENSE &&
                            uiState.selectedPaymentMethod == PaymentMethod.BALANCE_CARD -> uiState.selectedBalanceCard?.name
                            else -> null
                        },
                        actualAmount = if (uiState.isSettlement) parseAmountToDouble(uiState.actualAmount) else null,
                        settlementAmount = if (uiState.isSettlement) parseAmountToDouble(uiState.settlementAmount) else null,
                        isSettlement = uiState.isSettlement
                    )

                    listOf(transaction)
                }
            }

            // 저장 실행
            if (uiState.isEditMode && uiState.editTransaction != null) {
                // 편집 모드에서는 단일 거래만 업데이트
                if (transactions.size == 1) {
                    updateTransactionUseCase(transactions.first())
                }
            } else {
                // 새 거래 추가
                if (transactions.size == 1) {
                    saveTransactionUseCase(transactions.first())
                } else {
                    saveMultipleTransactionsUseCase(transactions)
                }
            }

            uiState = uiState.copy(
                isLoading = false,
                error = null
            )

            return transactions

        } catch (e: Exception) {
            uiState = uiState.copy(
                isLoading = false,
                error = e.message ?: "알 수 없는 오류가 발생했습니다."
            )
            return emptyList()
        }
    }
}