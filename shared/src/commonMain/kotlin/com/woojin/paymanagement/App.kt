package com.woojin.paymanagement

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.database.DatabaseDriverFactory
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.database.PayManagementDatabase
import com.woojin.paymanagement.ui.AddTransactionScreen
import com.woojin.paymanagement.ui.CalendarScreen
import com.woojin.paymanagement.ui.DateDetailScreen
import com.woojin.paymanagement.ui.PaydaySetupScreen
import com.woojin.paymanagement.ui.StatisticsScreen
import com.woojin.paymanagement.ui.TutorialScreen
import com.woojin.paymanagement.utils.PreferencesManager
import com.woojin.paymanagement.utils.PaydayAdjustment
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

@Composable
fun App(databaseDriverFactory: DatabaseDriverFactory, preferencesManager: PreferencesManager) {
    MaterialTheme {
        PayManagementApp(databaseDriverFactory, preferencesManager)
    }
}

@Composable
fun PayManagementApp(databaseDriverFactory: DatabaseDriverFactory, preferencesManager: PreferencesManager) {
    val database = remember { PayManagementDatabase(databaseDriverFactory.createDriver()) }
    val databaseHelper = remember { DatabaseHelper(database) }
    val scope = rememberCoroutineScope()

    // 초기 화면 결정 로직
    val initialScreen = when {
        preferencesManager.isFirstLaunch() -> Screen.Tutorial
        !preferencesManager.isPaydaySet() -> Screen.PaydaySetup
        else -> Screen.Calendar
    }
    var currentScreen by remember { mutableStateOf(initialScreen) }
    var selectedDate by remember { mutableStateOf<LocalDate>(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    var editTransaction by remember { mutableStateOf<Transaction?>(null) }
    var previousScreen by remember { mutableStateOf(Screen.Calendar) }
    var selectedPayPeriod by remember { mutableStateOf<com.woojin.paymanagement.utils.PayPeriod?>(null) }
    var currentCalendarPayPeriod by remember { mutableStateOf<com.woojin.paymanagement.utils.PayPeriod?>(null) }
    
    val transactions by databaseHelper.getAllTransactions().collectAsState(emptyList())
    val availableBalanceCards by databaseHelper.getActiveBalanceCards().collectAsState(emptyList())
    val availableGiftCards by databaseHelper.getActiveGiftCards().collectAsState(emptyList())
    
    when (currentScreen) {
        Screen.PaydaySetup -> {
            PaydaySetupScreen(
                onSetupComplete = { payday, adjustment ->
                    preferencesManager.setPayday(payday)
                    preferencesManager.setPaydayAdjustment(adjustment)
                    preferencesManager.setFirstLaunchCompleted()
                    currentScreen = Screen.Calendar
                }
            )
        }
        
        Screen.Tutorial -> {
            TutorialScreen(
                onTutorialComplete = {
                    preferencesManager.setTutorialCompleted()
                    currentScreen = Screen.PaydaySetup
                }
            )
        }
        
        Screen.Calendar -> {
            CalendarScreen(
                transactions = transactions,
                selectedDate = selectedDate,
                initialPayPeriod = currentCalendarPayPeriod,
                onDateSelected = { date ->
                    selectedDate = date
                },
                onDateDetailClick = { date ->
                    selectedDate = date
                    currentScreen = Screen.DateDetail
                },
                onStatisticsClick = { payPeriod ->
                    selectedPayPeriod = payPeriod
                    currentScreen = Screen.Statistics
                },
                onAddTransactionClick = {
                    editTransaction = null
                    previousScreen = Screen.Calendar
                    currentScreen = Screen.AddTransaction
                },
                onPayPeriodChanged = { payPeriod ->
                    currentCalendarPayPeriod = payPeriod
                },
                preferencesManager = preferencesManager
            )
        }
        
        Screen.Statistics -> {
            StatisticsScreen(
                transactions = transactions,
                availableBalanceCards = availableBalanceCards,
                availableGiftCards = availableGiftCards,
                initialPayPeriod = selectedPayPeriod,
                onBack = {
                    currentScreen = Screen.Calendar
                },
                preferencesManager = preferencesManager
            )
        }
        
        Screen.AddTransaction -> {
            AddTransactionScreen(
                selectedDate = selectedDate,
                editTransaction = editTransaction,
                availableBalanceCards = availableBalanceCards,
                availableGiftCards = availableGiftCards,
                onSave = { newTransactions ->
                    scope.launch {
                        if (editTransaction != null) {
                            // 편집 모드: 기존 거래 업데이트 (단일 거래)
                            databaseHelper.updateTransaction(newTransactions.first())
                        } else {
                            // 추가 모드: 새 거래들 추가 (복수 거래 가능)
                            newTransactions.forEach { transaction ->
                                // 잔액권 수입인 경우 잔액권 생성
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.INCOME &&
                                    transaction.incomeType == com.woojin.paymanagement.data.IncomeType.BALANCE_CARD &&
                                    transaction.balanceCardId != null && transaction.cardName != null) {
                                    val balanceCard = com.woojin.paymanagement.data.BalanceCard(
                                        id = transaction.balanceCardId!!,
                                        name = transaction.cardName!!,
                                        initialAmount = transaction.amount,
                                        currentBalance = transaction.amount,
                                        createdDate = transaction.date,
                                        isActive = true
                                    )
                                    databaseHelper.insertBalanceCard(balanceCard)
                                }

                                // 상품권 수입인 경우 상품권 생성
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.INCOME &&
                                    transaction.incomeType == com.woojin.paymanagement.data.IncomeType.GIFT_CARD &&
                                    transaction.giftCardId != null && transaction.cardName != null) {
                                    val giftCard = com.woojin.paymanagement.data.GiftCard(
                                        id = transaction.giftCardId!!,
                                        name = transaction.cardName!!,
                                        totalAmount = transaction.amount,
                                        usedAmount = 0.0,
                                        createdDate = transaction.date,
                                        isActive = true
                                    )
                                    databaseHelper.insertGiftCard(giftCard)
                                }

                                // 잔액권 지출인 경우 잔액권 잔액 업데이트
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.EXPENSE &&
                                    transaction.paymentMethod == com.woojin.paymanagement.data.PaymentMethod.BALANCE_CARD &&
                                    transaction.balanceCardId != null) {
                                    // 데이터베이스에서 최신 카드 정보 조회
                                    val currentCard = databaseHelper.getBalanceCardById(transaction.balanceCardId!!)
                                    if (currentCard != null) {
                                        val newBalance = currentCard.currentBalance - transaction.amount
                                        databaseHelper.updateBalanceCardBalance(
                                            id = currentCard.id,
                                            currentBalance = newBalance,
                                            isActive = newBalance > 0
                                        )
                                    }
                                }

                                // 상품권 지출인 경우 상품권 사용량 업데이트
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.EXPENSE &&
                                    transaction.paymentMethod == com.woojin.paymanagement.data.PaymentMethod.GIFT_CARD &&
                                    transaction.giftCardId != null) {
                                    // 데이터베이스에서 최신 카드 정보 조회
                                    val currentCard = databaseHelper.getGiftCardById(transaction.giftCardId!!)
                                    if (currentCard != null) {
                                        val newUsedAmount = currentCard.usedAmount + transaction.amount
                                        // 상품권이 한 번 사용되면 완전히 비활성화 (환급 발생)
                                        databaseHelper.updateGiftCardUsage(
                                            id = currentCard.id,
                                            usedAmount = newUsedAmount,
                                            isActive = false // 한 번 사용되면 완전히 비활성화
                                        )
                                    }
                                }

                                databaseHelper.insertTransaction(transaction)
                            }
                        }
                    }
                    currentScreen = previousScreen
                },
                onCancel = {
                    currentScreen = previousScreen
                }
            )
        }
        
        Screen.DateDetail -> {
            DateDetailScreen(
                selectedDate = selectedDate,
                transactions = transactions,
                onBack = {
                    currentScreen = Screen.Calendar
                },
                onEditTransaction = { transaction ->
                    editTransaction = transaction
                    previousScreen = Screen.DateDetail
                    currentScreen = Screen.AddTransaction
                },
                onDeleteTransaction = { transaction ->
                    scope.launch {
                        databaseHelper.deleteTransaction(transaction.id)
                    }
                },
                onAddTransaction = {
                    editTransaction = null
                    previousScreen = Screen.DateDetail
                    currentScreen = Screen.AddTransaction
                }
            )
        }
        
        Screen.EditTransaction -> {
            // EditTransaction은 AddTransaction과 동일하게 처리
            AddTransactionScreen(
                selectedDate = selectedDate,
                editTransaction = editTransaction,
                availableBalanceCards = availableBalanceCards,
                availableGiftCards = availableGiftCards,
                onSave = { newTransactions ->
                    scope.launch {
                        if (editTransaction != null) {
                            databaseHelper.updateTransaction(newTransactions.first())
                        } else {
                            newTransactions.forEach { transaction ->
                                // 잔액권 수입인 경우 잔액권 생성
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.INCOME &&
                                    transaction.incomeType == com.woojin.paymanagement.data.IncomeType.BALANCE_CARD &&
                                    transaction.balanceCardId != null && transaction.cardName != null) {
                                    val balanceCard = com.woojin.paymanagement.data.BalanceCard(
                                        id = transaction.balanceCardId!!,
                                        name = transaction.cardName!!,
                                        initialAmount = transaction.amount,
                                        currentBalance = transaction.amount,
                                        createdDate = transaction.date,
                                        isActive = true
                                    )
                                    databaseHelper.insertBalanceCard(balanceCard)
                                }

                                // 상품권 수입인 경우 상품권 생성
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.INCOME &&
                                    transaction.incomeType == com.woojin.paymanagement.data.IncomeType.GIFT_CARD &&
                                    transaction.giftCardId != null && transaction.cardName != null) {
                                    val giftCard = com.woojin.paymanagement.data.GiftCard(
                                        id = transaction.giftCardId!!,
                                        name = transaction.cardName!!,
                                        totalAmount = transaction.amount,
                                        usedAmount = 0.0,
                                        createdDate = transaction.date,
                                        isActive = true
                                    )
                                    databaseHelper.insertGiftCard(giftCard)
                                }

                                // 잔액권 지출인 경우 잔액권 잔액 업데이트
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.EXPENSE &&
                                    transaction.paymentMethod == com.woojin.paymanagement.data.PaymentMethod.BALANCE_CARD &&
                                    transaction.balanceCardId != null) {
                                    // 데이터베이스에서 최신 카드 정보 조회
                                    val currentCard = databaseHelper.getBalanceCardById(transaction.balanceCardId!!)
                                    if (currentCard != null) {
                                        val newBalance = currentCard.currentBalance - transaction.amount
                                        databaseHelper.updateBalanceCardBalance(
                                            id = currentCard.id,
                                            currentBalance = newBalance,
                                            isActive = newBalance > 0
                                        )
                                    }
                                }

                                // 상품권 지출인 경우 상품권 사용량 업데이트
                                if (transaction.type == com.woojin.paymanagement.data.TransactionType.EXPENSE &&
                                    transaction.paymentMethod == com.woojin.paymanagement.data.PaymentMethod.GIFT_CARD &&
                                    transaction.giftCardId != null) {
                                    // 데이터베이스에서 최신 카드 정보 조회
                                    val currentCard = databaseHelper.getGiftCardById(transaction.giftCardId!!)
                                    if (currentCard != null) {
                                        val newUsedAmount = currentCard.usedAmount + transaction.amount
                                        // 상품권이 한 번 사용되면 완전히 비활성화 (환급 발생)
                                        databaseHelper.updateGiftCardUsage(
                                            id = currentCard.id,
                                            usedAmount = newUsedAmount,
                                            isActive = false // 한 번 사용되면 완전히 비활성화
                                        )
                                    }
                                }

                                databaseHelper.insertTransaction(transaction)
                            }
                        }
                    }
                    currentScreen = Screen.DateDetail
                },
                onCancel = {
                    currentScreen = Screen.DateDetail
                }
            )
        }
    }
}

enum class Screen {
    PaydaySetup,
    Tutorial,
    Calendar,
    Statistics,
    AddTransaction,
    DateDetail,
    EditTransaction
}