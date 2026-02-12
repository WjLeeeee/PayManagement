package com.woojin.paymanagement.strings

/**
 * Interface defining all strings used in the app.
 * Implementations for each language must provide all properties and functions.
 */
interface AppStrings {
    // ===== Common =====
    val appName: String
    val confirm: String
    val cancel: String
    val save: String
    val delete: String
    val edit: String
    val close: String
    val back: String
    val next: String
    val done: String
    val start: String
    val settings: String
    val notice: String
    val warning: String
    val error: String
    val success: String
    val loading: String
    val empty: String
    val all: String
    val none: String
    val add: String
    val search: String
    val reset: String
    val apply: String
    val select: String
    val change: String
    val manage: String
    val fold: String
    val expand: String

    // ===== Numbers & Currency =====
    val currencySymbol: String
    fun amountWithUnit(amount: String): String
    fun won(amount: Long): String
    fun percentage(value: Int): String
    fun dayOfMonth(day: Int): String

    // ===== Date & Time =====
    fun monthYear(year: Int, month: Int): String
    fun fullDate(year: Int, month: Int, day: Int): String
    fun shortDate(month: Int, day: Int): String
    fun dayWithWeekday(day: Int, weekday: String): String
    val sunday: String
    val monday: String
    val tuesday: String
    val wednesday: String
    val thursday: String
    val friday: String
    val saturday: String
    fun weekdayShort(weekday: String): String
    val today: String
    val yesterday: String
    val tomorrow: String

    // ===== Transaction Types =====
    val income: String
    val expense: String
    val transfer: String
    val transactionType: String

    // ===== Categories =====
    val category: String
    val categoryManagement: String
    val addIncomeCategory: String
    val addExpenseCategory: String
    val addCategory: String
    val deleteCategory: String
    val editCategory: String
    val incomeCategories: String
    val expenseCategories: String
    val categoryName: String
    val selectCategory: String
    val noCategorySelected: String
    val uncategorized: String
    val categoryColor: String
    val categoryIcon: String
    val defaultCategories: String
    val customCategories: String
    val cannotDeleteDefaultCategory: String
    val categoryInUseCannotDelete: String
    val deleteCategoryConfirm: String

    // Default category names
    val categorySalary: String
    val categoryBonus: String
    val categoryInvestment: String
    val categoryAllowance: String
    val categoryOtherIncome: String
    val categoryFood: String
    val categoryTransport: String
    val categoryShopping: String
    val categoryEntertainment: String
    val categoryHealth: String
    val categoryEducation: String
    val categoryHousing: String
    val categoryTelecom: String
    val categoryOtherExpense: String

    // ===== Payday Setup =====
    val paydaySetup: String
    val paydaySetupTitle: String
    val paydaySetupSubtitle: String
    val paydaySetupDescription: String
    val selectPaydayPrompt: String
    val paydayOnWeekendHoliday: String
    val payBeforeWeekday: String
    val payAfterWeekday: String
    val setupComplete: String
    val selectPayday: String
    val paydayChange: String
    val currentPayday: String
    val paydayAdjustment: String
    val beforeWeekend: String
    val afterWeekend: String
    val beforeWeekendDesc: String
    val afterWeekendDesc: String
    val monthlySalary: String
    val enterSalary: String
    val salaryOptional: String
    val confirmPayday: String
    val paydayChangeConfirm: String
    val paydayChangeDesc: String
    val goToPaydaySettings: String
    val welcomeMessage: String
    val welcomeSubMessage: String
    fun paydayDisplay(day: Int): String

    // ===== Calendar Screen =====
    val payPeriodSummary: String
    val remainingBudget: String
    val totalIncome: String
    val totalExpense: String
    val balance: String
    val dailyAverage: String
    val remainingDays: String
    val noTransactions: String
    val addFirstTransaction: String
    val viewTransactions: String
    val tapToAddTransaction: String
    fun remainingDaysCount(days: Int): String
    fun daysRemaining(days: Int): String
    fun dailyBudgetRecommend(amount: String): String
    val budgetExceeded: String
    val withinBudget: String
    val showPayPeriod: String
    val hidePayPeriod: String
    val viewDetails: String
    val payPeriodStart: String
    val payPeriodEnd: String
    fun payPeriodRange(startMonth: Int, startDay: Int, endMonth: Int, endDay: Int): String
    val incomeExpenseRatio: String
    val spendingPace: String
    val onTrack: String
    val overSpending: String
    val underSpending: String
    val weekdaysShort: List<String>
    val selectDateToMove: String
    val selectDateToViewMemo: String
    val tapCalendarToViewMemo: String
    val cardPaymentHistory: String
    val parsingPermission: String
    val pushPermission: String
    val yearLabel: String
    val monthLabel: String
    fun yearDisplay(year: Int): String
    fun monthDisplayShort(month: Int): String
    val previousYear: String
    val nextYear: String
    val previousMonthLabel: String
    val nextMonthLabel: String
    fun dateTransactionHeader(month: Int, day: Int): String
    val noTransactionsOnDate: String

    // ===== Add/Edit Transaction =====
    val addTransaction: String
    val editTransaction: String
    val transactionDate: String
    val transactionAmount: String
    val transactionMemo: String
    val enterAmount: String
    val enterMemo: String
    val selectDate: String
    val selectPaymentMethod: String
    val cash: String
    val card: String
    val bankTransfer: String
    val giftCard: String
    val paymentMethod: String
    val transactionSaved: String
    val transactionDeleted: String
    val deleteTransactionConfirm: String
    val amountRequired: String
    val invalidAmount: String
    val memo: String
    val noMemo: String
    val transactionDetails: String
    val duplicateTransaction: String
    val similarTransactionExists: String

    // ===== Date Detail Screen =====
    val dateDetail: String
    val transactionsForDay: String
    fun transactionCount(count: Int): String
    val noTransactionsForDay: String
    val dailySummary: String
    val totalForDay: String
    val incomeForDay: String
    val expenseForDay: String
    val deleteTransactionTitle: String
    val cannotDeleteTransaction: String
    val recurringShort: String
    val saveAsRecurring: String
    fun transactionListHeader(count: Int): String

    // ===== Statistics Screen =====
    val statistics: String
    val statisticsTitle: String
    val monthlyStatistics: String
    val categoryStatistics: String
    val paymentMethodStatistics: String
    val spendingTrend: String
    val incomeVsExpense: String
    val topCategories: String
    val topExpenseCategories: String
    val topIncomeCategories: String
    val noDataForPeriod: String
    val averageDaily: String
    val averageMonthly: String
    val highestSpendingDay: String
    val lowestSpendingDay: String
    val totalTransactions: String
    val chartView: String
    val listView: String
    val pieChart: String
    val barChart: String
    val lineChart: String
    fun categoryPercentage(category: String, percentage: Int): String

    // ===== Budget Settings =====
    val budgetSettings: String
    val setBudget: String
    val editBudget: String
    val deleteBudget: String
    val monthlyBudget: String
    val categoryBudget: String
    val budgetAmount: String
    val enterBudgetAmount: String
    val budgetRemaining: String
    val budgetUsed: String
    val budgetExceededWarning: String
    val noBudgetSet: String
    val budgetSaved: String
    val budgetDeleted: String
    val deleteBudgetConfirm: String
    val viewBudgetProgress: String
    fun budgetProgress(used: String, total: String): String
    fun budgetRemainingAmount(amount: String): String
    fun budgetExceededAmount(amount: String): String
    val budgetExceededNotification: String
    val totalBudget: String
    val usedAmount: String
    val remainingAmount: String
    val budgetUtilization: String

    // ===== Card Management =====
    val cardManagement: String
    val addCard: String
    val editCard: String
    val deleteCard: String
    val cardName: String
    val cardType: String
    val creditCard: String
    val debitCard: String
    val prepaidCard: String
    val balanceCard: String
    val cardBalance: String
    val enterCardName: String
    val enterCardBalance: String
    val cardList: String
    val noCards: String
    val addFirstCard: String
    val cardSaved: String
    val cardDeleted: String
    val deleteCardConfirm: String
    val cardDetails: String
    val lastUsed: String
    val totalSpent: String
    val balanceCardManagement: String
    val giftCardManagement: String
    val addBalanceCard: String
    val addGiftCard: String
    val initialBalance: String
    val currentBalance: String
    val usedBalance: String
    val expirationDate: String
    val noExpirationDate: String
    val setExpirationDate: String
    val cardExpired: String
    val cardExpiringSoon: String
    val activeCards: String
    val inactiveCards: String
    val markAsUsed: String
    val activateCard: String

    // ===== Recurring Transactions =====
    val recurringTransactions: String
    val addRecurringTransaction: String
    val editRecurringTransaction: String
    val deleteRecurringTransaction: String
    val recurringTransactionName: String
    val recurrencePattern: String
    val daily: String
    val weekly: String
    val monthly: String
    val yearly: String
    val everyDay: String
    val everyWeek: String
    val everyMonth: String
    val everyYear: String
    val selectRecurrenceDay: String
    val recurringAmount: String
    val startDate: String
    val endDate: String
    val noEndDate: String
    val nextOccurrence: String
    val lastExecuted: String
    val neverExecuted: String
    val recurringTransactionSaved: String
    val recurringTransactionDeleted: String
    val deleteRecurringTransactionConfirm: String
    val recurringTransactionPaused: String
    val recurringTransactionResumed: String
    val pauseRecurring: String
    val resumeRecurring: String
    val executeNow: String
    val skipNext: String
    val noRecurringTransactions: String
    val addFirstRecurringTransaction: String
    val recurringTransactionDetails: String
    val autoAdd: String
    val manualConfirm: String
    val autoAddDesc: String
    val manualConfirmDesc: String
    fun recurringDayOfMonth(day: Int): String
    fun recurringDayOfWeek(weekday: String): String
    val todaysRecurringTransactions: String
    val pendingRecurringTransactions: String

    // ===== Parsed Transactions (SMS/Notification) =====
    val parsedTransactions: String
    val cardNotificationParsing: String
    val parsedTransactionsList: String
    val addToParsed: String
    val ignoreParsed: String
    val allParsed: String
    val parsedFromNotification: String
    val noParsedTransactions: String
    val parsedTransactionAdded: String
    val parsedTransactionIgnored: String
    val deleteAllParsed: String
    val deleteAllParsedConfirm: String
    val cardNotificationDetection: String
    val cardNotificationDetectionDesc: String
    val appNotification: String
    val appNotificationDesc: String

    // ===== Theme Settings =====
    val themeSettings: String
    val theme: String
    val lightTheme: String
    val darkTheme: String
    val systemTheme: String
    val themeLight: String
    val themeDark: String
    val themeSystem: String

    // ===== Language Settings =====
    val languageSettings: String
    val language: String
    val korean: String
    val english: String
    val languageChangeRestart: String
    val selectLanguage: String

    // ===== Data Management =====
    val dataManagement: String
    val exportData: String
    val importData: String
    val exportToJson: String
    val importFromJson: String
    val exportDataDesc: String
    val importDataDesc: String
    val exportSuccess: String
    val exportFailed: String
    val importSuccess: String
    val importFailed: String
    val importConfirm: String
    val importConfirmDesc: String
    val importWarning: String
    val existingDataWillBeDeleted: String
    val fileLoadFailed: String
    fun importResult(success: Int, failed: Int): String
    val backupData: String
    val restoreData: String

    // ===== Calculator =====
    val calculator: String
    val calculate: String
    val clear: String
    val clearAll: String
    val equals: String
    val useResult: String
    val calculatorResult: String

    // ===== Tip Donation =====
    val tipDonation: String
    val tipDonationTitle: String
    val tipDonationDesc: String
    val supportDeveloper: String
    val smallTip: String
    val mediumTip: String
    val largeTip: String
    val thankYouForTip: String
    val tipPurchaseFailed: String
    val tipAlreadyPurchased: String

    // ===== Ad Removal =====
    val adRemoval: String
    val removeAds: String
    val adRemovalDesc: String
    val adRemovalBenefit: String
    val adFreeExperience: String
    val purchaseAdRemoval: String
    val adRemovalActive: String
    val adRemovalExpired: String
    fun adRemovalExpiresIn(days: Int): String
    val restorePurchase: String
    val purchaseFailed: String
    val purchaseSuccess: String

    // ===== Coupon =====
    val coupon: String
    val enterCoupon: String
    val couponCode: String
    val applyCoupon: String
    val invalidCoupon: String
    val couponAlreadyUsed: String
    val couponApplied: String
    val couponExpired: String
    val couponNotFound: String
    val enterCouponCode: String

    // ===== Monthly Comparison =====
    val monthlyComparison: String
    val compareMonths: String
    val previousMonth: String
    val currentMonth: String
    val difference: String
    val increased: String
    val decreased: String
    val noChange: String
    fun comparedToLastMonth(amount: String): String
    fun increasePercentage(percentage: Int): String
    fun decreasePercentage(percentage: Int): String
    val previousPayPeriod: String
    val currentPayPeriod: String
    val payPeriodComparison: String
    val newPayPeriodStarted: String
    val compareWithPreviousPeriod: String

    // ===== Tutorial =====
    val tutorialWelcome: String
    val tutorialCalendar: String
    val tutorialAddTransaction: String
    val tutorialStatistics: String
    val tutorialBudget: String
    val tutorialSkip: String
    val tutorialNext: String
    val tutorialFinish: String
    val tutorialDontShowAgain: String
    val tapHereToAdd: String
    val swipeToNavigate: String
    val longPressForOptions: String

    // ===== Permissions =====
    val permissionRequired: String
    val notificationListenerPermission: String
    val notificationListenerPermissionDesc: String
    val postNotificationPermission: String
    val postNotificationPermissionDesc: String
    val goToSettings: String
    val permissionDenied: String
    val permissionGranted: String

    // ===== Errors & Validation =====
    val networkError: String
    val unknownError: String
    val tryAgain: String
    val dataLoadFailed: String
    val dataSaveFailed: String
    val requiredField: String
    val invalidInput: String
    val valueTooLarge: String
    val valueTooSmall: String
    val pleaseEnterValidAmount: String
    val pleaseSelectCategory: String
    val connectionFailed: String
    val timeout: String

    // ===== Drawer Menu =====
    val pushNotifications: String
    val aboutApp: String
    val version: String
    val contactSupport: String
    val rateApp: String
    val privacyPolicy: String
    val termsOfService: String
    val appExit: String
    val exitConfirm: String
    val exitConfirmMessage: String
    val hideMoneyAmounts: String
    val showMoneyAmounts: String
    val moneyVisibility: String
    fun addIncomeExpenseCategory(type: String): String

    // ===== Snackbar & Toast Messages =====
    val changesSaved: String
    val changesDiscarded: String
    val operationSuccess: String
    val operationFailed: String
    val copiedToClipboard: String
    val undoAction: String
    val actionCancelled: String

    // ===== Empty States =====
    val noDataYet: String
    val startByAdding: String
    val nothingToShow: String
    val emptyCategory: String
    val emptyCardList: String
    val emptyTransactionList: String
    val emptyBudgetList: String
    val emptyRecurringList: String
    val emptyStatistics: String

    // ===== Accessibility =====
    val closeDialog: String
    val openMenu: String
    val closeMenu: String
    val goBack: String
    val moreOptions: String
    val refresh: String
    val scrollUp: String
    val scrollDown: String
    val expandSection: String
    val collapseSection: String

    // ===== Statistics (additional) =====
    val incomeAnalysis: String
    val expenseAnalysis: String
    val investmentActivityAnalysis: String
    val investmentActivitySummary: String
    val investment: String
    val stopLoss: String
    val profitTaking: String
    val dividend: String
    val paymentMethodAnalysis: String
    val noTransactionsForPeriod: String
    val differenceAmount: String
    val charge: String
    val usage: String
    val purchase: String
    val settlementIncome: String
    val actualUsage: String
    val other: String

    // ===== Budget (additional) =====
    val budgetManagement: String
    val usageStatus: String
    val overallProgress: String
    val spendingPlan: String
    val plannedTotal: String
    val salaryLabel: String
    val budgetLabel: String
    val usedLabel: String
    val remainingLabel: String
    val exceededLabel: String
    val noBudgetCategoriesMessage: String
    val addCategoryLabel: String
    val allocatedAmount: String
    val enterAmountPlaceholder: String
    val selectCategoriesMultiple: String
    val groupNameInput: String
    val groupNameLabel: String
    val memoOptional: String
    val noCategoriesAvailable: String
    val includedCategories: String
    val categorySpending: String
    val totalLabel: String
    val editSalary: String
    val setSalary: String
    val addCategoryBudget: String
    fun editBudgetTitle(emoji: String, name: String): String

    // ===== Add Transaction (additional) =====
    val savingTransaction: String
    val dateLabel: String
    val merchantLabel: String
    val incomeType: String
    val newBalanceCard: String
    val chargeExistingBalanceCard: String
    val balanceCardNameHint: String
    val giftCardNameHint: String
    val selectBalanceCardToCharge: String
    val selectBalanceCardLabel: String
    val selectGiftCardLabel: String
    val dutchPaySettlement: String
    val settlementAmountLabel: String
    val settlementDescription: String
    fun balanceCardFullUsage(amount: Int, remaining: Int): String
    fun balanceCardPartialUsage(cardAmount: Int, cashAmount: Int): String
    fun giftCardFullUsage(amount: Int): String
    fun giftCardRefundUsage(amount: Int, refund: Int): String
    fun giftCardPartialUsage(cardAmount: Int, cashAmount: Int): String

    // ===== Calculator (additional) =====
    val periodCalculator: String
    val periodSetting: String
    val editPeriod: String
    val totalAmount: String
    val transactionCountLabel: String
    val averageAmount: String
    val selectStartDate: String
    val selectEndDate: String
    val selectionComplete: String
    val transactionDetail: String

    // ===== Theme (additional) =====
    val systemThemeDesc: String

    // ===== Coupon (additional) =====
    val couponCodeInput: String
    val couponDesc: String
    val applyingCoupon: String
    val applyCouponButton: String
    val couponApplyComplete: String
    val couponSuccessMessage: String

    // ===== Ad Removal (additional) =====
    val useWithoutAds: String
    val adRemovalLongDesc: String
    val oneDayPass: String
    val threeDayPass: String
    val sevenDayPass: String
    val thirtyDayPass: String
    val processingPayment: String
    fun payAmountButton(amount: String): String
    val selectPeriod: String
    val purchaseComplete: String
    val adRemovalCompleteMessage: String
    val paymentFailedTitle: String

    // ===== Tip Donation (additional) =====
    val tipDonationScreenTitle: String
    val supportDeveloperTitle: String
    val tipDonationLongDesc: String
    val buyCoffee: String
    val buyLunch: String
    val buyDinner: String
    val selectAmountPrompt: String
    val thankYouTitle: String
    val thankYouMessage: String

    // ===== Monthly Comparison (additional) =====
    val categoryComparison: String
    val noComparisonData: String
    val totalExpenseComparison: String
    val previous: String
    val current: String
    val increasedArrow: String
    val savingsArrow: String
    val same: String

    // ===== Parsed Transactions (additional) =====
    val parsedTransactionDesc: String
    fun errorWithMessage(message: String): String
    val cardNotificationAutoDisplay: String
    val notificationOn: String
    val notificationOff: String

    // ===== Category Management (additional) =====
    val emojiLabel: String
    val continueAction: String
    fun deleteCategoryConfirmMessage(emoji: String, name: String): String

    // ===== Card Management (additional) =====
    val balanceGiftCardManagement: String
    val activeTab: String
    val completedTab: String
    val noActiveCards: String
    val noCompletedCards: String
    fun balanceDisplay(amount: String): String
    val transactionHistory: String
    val noTransactionHistory: String
    fun incomeAmountDisplay(amount: String): String
    fun expenseAmountDisplay(amount: String): String

    // ===== Recurring Transaction (additional) =====
    val recurringTransactionManagement: String
    val todayItems: String
    val noRegisteredRecurringTransactions: String
    fun paymentMethodDisplay(method: String): String
    val cashCheckCard: String
    val recurringPatternLabel: String
    val whichDayOfMonth: String
    val whichDayOfWeek: String
    val weekendHandling: String
    val applyAsIs: String
    val moveToPreviousWeekday: String
    val moveToNextWeekday: String
    val noCategoriesRegistered: String
    val memoOptionalShort: String

    // ===== Custom Payment Method Management =====
    val paymentMethodManagement: String
    val addPaymentMethod: String
    val editPaymentMethod: String
    val deletePaymentMethod: String
    val paymentMethodName: String
    val paymentMethodManagementDesc: String
    val defaultPaymentMethods: String
    val customPaymentMethods: String
    val unspecifiedCard: String
    fun deletePaymentMethodConfirmMessage(name: String): String
    fun totalPaymentMethodCount(defaultCount: Int, customCount: Int): String
    fun defaultPaymentMethodCount(count: Int): String
    fun customPaymentMethodCount(count: Int): String
    val selectCard: String
    val defaultCard: String
    val setAsDefaultCard: String
    val cardBreakdown: String
    val transactionTools: String
}
