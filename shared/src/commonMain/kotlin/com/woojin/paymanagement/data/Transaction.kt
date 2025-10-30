package com.woojin.paymanagement.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val merchant: String? = null,        // 사용처 (지출일 때만 필수)
    val memo: String,
    val date: LocalDate,
    val incomeType: IncomeType? = null, // 수입일 때만 사용
    val paymentMethod: PaymentMethod? = null, // 지출일 때만 사용
    val balanceCardId: String? = null,   // 잔액권 ID (수입 시 생성, 지출 시 사용)
    val giftCardId: String? = null,      // 상품권 ID (수입 시 생성, 지출 시 사용)
    val cardName: String? = null,        // 잔액권/상품권 이름
    val actualAmount: Double? = null,    // 실제 결제액 (더치페이 시)
    val settlementAmount: Double? = null, // 정산받은 금액 (더치페이 시)
    val isSettlement: Boolean = false    // 더치페이 여부
)

@Serializable
enum class TransactionType {
    INCOME,
    EXPENSE
}

@Serializable
enum class IncomeType {
    CASH,        // 현금
    BALANCE_CARD, // 잔액권 (편의점 상품권 등, 잔액 관리 필요)
    GIFT_CARD    // 상품권 (신세계상품권 등, 80% 이상 사용 시 나머지는 현금 환급)
}

@Serializable
enum class PaymentMethod {
    CASH,        // 현금 지출 (체크카드 포함)
    CARD,        // 신용카드 지출
    BALANCE_CARD, // 잔액권 사용
    GIFT_CARD    // 상품권 사용
}

@Serializable
data class BalanceCard(
    val id: String,
    val name: String,        // 상품권 이름 (예: "편의점 상품권")
    val initialAmount: Double, // 초기 금액
    val currentBalance: Double, // 현재 잔액
    val createdDate: LocalDate, // 생성일
    val isActive: Boolean = true // 활성 상태 (잔액이 0이 되면 false)
)

@Serializable
data class GiftCard(
    val id: String,
    val name: String,        // 상품권 이름 (예: "신세계 상품권")
    val totalAmount: Double, // 총 금액
    val usedAmount: Double,  // 사용된 금액
    val createdDate: LocalDate, // 생성일
    val isActive: Boolean = true, // 활성 상태
    val minimumUsageRate: Double = 0.8 // 최소 사용률 (기본 80%)
) {
    val remainingAmount: Double
        get() = totalAmount - usedAmount

    val canUse: Boolean
        get() = isActive && remainingAmount > 0
}

@Serializable
data class GiftCardTransactionResult(
    val transactions: List<Transaction>, // 생성된 거래들 (지출 + 환급)
    val updatedGiftCard: GiftCard? = null, // 업데이트된 상품권 (완전 사용 시 null)
    val cashRefund: Double = 0.0 // 현금 환급 금액
)

@Serializable
data class BalanceCardTransactionResult(
    val transactions: List<Transaction>, // 생성된 거래들
    val updatedBalanceCard: BalanceCard? = null, // 업데이트된 잔액권 (잔액이 0이 되면 null)
    val cashNeeded: Double = 0.0 // 추가로 필요한 현금
)

@Serializable
data class PaymentMethodSummary(
    val cashIncome: Double = 0.0,
    val cashExpense: Double = 0.0,
    val cardExpense: Double = 0.0,
    val cardActualExpense: Double = 0.0, // 더치페이 포함 실제 카드 사용액
    val settlementIncome: Double = 0.0, // 더치페이로 받은 금액
    val balanceCards: List<BalanceCardSummary> = emptyList(),
    val giftCards: List<GiftCardSummary> = emptyList()
)

@Serializable
data class BalanceCardSummary(
    val id: String,
    val name: String,
    val income: Double = 0.0, // 이 기간에 추가된 잔액권
    val expense: Double = 0.0, // 이 기간에 사용된 잔액권
    val currentBalance: Double = 0.0 // 현재 잔액
)

@Serializable
data class GiftCardSummary(
    val id: String,
    val name: String,
    val income: Double = 0.0, // 이 기간에 추가된 상품권
    val expense: Double = 0.0, // 이 기간에 사용된 상품권
    val currentBalance: Double = 0.0 // 현재 잔액
)

object GiftCardUtils {
    /**
     * 상품권을 사용하여 지출을 처리합니다.
     * @param giftCard 사용할 상품권
     * @param expenseAmount 지출 금액
     * @param baseTransaction 기본 거래 정보 (카테고리, 메모, 날짜 등)
     * @return 처리 결과 (거래 목록, 업데이트된 상품권, 환급 금액)
     */
    fun processGiftCardExpense(
        giftCard: GiftCard,
        expenseAmount: Double,
        baseTransaction: Transaction
    ): GiftCardTransactionResult {
        val availableAmount = giftCard.remainingAmount

        return when {
            // 상품권 잔액이 지출보다 많은 경우: 지출 + 환급
            availableAmount > expenseAmount -> {
                val giftCardUsed = expenseAmount
                val cashRefund = availableAmount - expenseAmount

                val transactions = mutableListOf<Transaction>()

                // 상품권 지출 거래
                transactions.add(
                    baseTransaction.copy(
                        id = kotlin.random.Random.nextLong().toString(),
                        amount = giftCardUsed,
                        paymentMethod = PaymentMethod.GIFT_CARD,
                        giftCardId = giftCard.id,
                        cardName = giftCard.name
                    )
                )

                // 현금 환급 거래 (수입)
                if (cashRefund > 0) {
                    transactions.add(
                        Transaction(
                            id = kotlin.random.Random.nextLong().toString(),
                            amount = cashRefund,
                            type = TransactionType.INCOME,
                            category = "상품권 환급",
                            memo = "${giftCard.name} 환급",
                            date = baseTransaction.date,
                            incomeType = IncomeType.CASH
                        )
                    )
                }

                GiftCardTransactionResult(
                    transactions = transactions,
                    updatedGiftCard = null, // 완전히 사용됨
                    cashRefund = cashRefund
                )
            }

            // 상품권 잔액이 지출보다 적은 경우: 상품권 전액 사용 + 현금 지출
            availableAmount < expenseAmount -> {
                val giftCardUsed = availableAmount
                val cashNeeded = expenseAmount - availableAmount

                val transactions = mutableListOf<Transaction>()

                // 상품권 지출 거래
                transactions.add(
                    baseTransaction.copy(
                        id = kotlin.random.Random.nextLong().toString(),
                        amount = giftCardUsed,
                        paymentMethod = PaymentMethod.GIFT_CARD,
                        giftCardId = giftCard.id,
                        cardName = giftCard.name
                    )
                )

                // 현금 지출 거래
                transactions.add(
                    baseTransaction.copy(
                        id = kotlin.random.Random.nextLong().toString(),
                        amount = cashNeeded,
                        paymentMethod = PaymentMethod.CASH
                    )
                )

                GiftCardTransactionResult(
                    transactions = transactions,
                    updatedGiftCard = null, // 완전히 사용됨
                    cashRefund = 0.0
                )
            }

            // 상품권 잔액과 지출이 정확히 같은 경우: 상품권만 사용
            else -> {
                val transactions = listOf(
                    baseTransaction.copy(
                        id = kotlin.random.Random.nextLong().toString(),
                        amount = expenseAmount,
                        paymentMethod = PaymentMethod.GIFT_CARD,
                        giftCardId = giftCard.id,
                        cardName = giftCard.name
                    )
                )

                GiftCardTransactionResult(
                    transactions = transactions,
                    updatedGiftCard = null, // 완전히 사용됨
                    cashRefund = 0.0
                )
            }
        }
    }
}

object BalanceCardUtils {
    /**
     * 잔액권을 사용하여 지출을 처리합니다.
     * @param balanceCard 사용할 잔액권
     * @param expenseAmount 지출 금액
     * @param baseTransaction 기본 거래 정보 (카테고리, 메모, 날짜 등)
     * @return 처리 결과 (거래 목록, 업데이트된 잔액권, 추가 현금 필요 여부)
     */
    fun processBalanceCardExpense(
        balanceCard: BalanceCard,
        expenseAmount: Double,
        baseTransaction: Transaction
    ): BalanceCardTransactionResult {
        val availableBalance = balanceCard.currentBalance

        return when {
            // 잔액권 잔액이 지출보다 많거나 같은 경우: 잔액권에서만 차감
            availableBalance >= expenseAmount -> {
                val transactions = listOf(
                    baseTransaction.copy(
                        id = kotlin.random.Random.nextLong().toString(),
                        amount = expenseAmount,
                        paymentMethod = PaymentMethod.BALANCE_CARD,
                        balanceCardId = balanceCard.id,
                        cardName = balanceCard.name
                    )
                )

                val newBalance = availableBalance - expenseAmount
                val updatedCard = if (newBalance > 0) {
                    balanceCard.copy(
                        currentBalance = newBalance,
                        isActive = true
                    )
                } else {
                    null // 잔액이 0이 되면 비활성화
                }

                BalanceCardTransactionResult(
                    transactions = transactions,
                    updatedBalanceCard = updatedCard,
                    cashNeeded = 0.0
                )
            }

            // 잔액권 잔액이 지출보다 적은 경우: 잔액권 전액 사용 + 현금 지출
            else -> {
                val balanceCardUsed = availableBalance
                val cashNeeded = expenseAmount - availableBalance

                val transactions = mutableListOf<Transaction>()

                // 잔액권 지출 거래
                transactions.add(
                    baseTransaction.copy(
                        id = kotlin.random.Random.nextLong().toString(),
                        amount = balanceCardUsed,
                        paymentMethod = PaymentMethod.BALANCE_CARD,
                        balanceCardId = balanceCard.id,
                        cardName = balanceCard.name
                    )
                )

                // 현금 지출 거래
                transactions.add(
                    baseTransaction.copy(
                        id = kotlin.random.Random.nextLong().toString(),
                        amount = cashNeeded,
                        paymentMethod = PaymentMethod.CASH
                    )
                )

                BalanceCardTransactionResult(
                    transactions = transactions,
                    updatedBalanceCard = null, // 잔액이 0이 되어 비활성화
                    cashNeeded = cashNeeded
                )
            }
        }
    }
}

object PaymentMethodAnalyzer {
    /**
     * 거래 목록에서 결제 수단별 통계를 계산합니다.
     */
    fun analyzePaymentMethods(
        transactions: List<Transaction>,
        availableBalanceCards: List<BalanceCard> = emptyList(),
        availableGiftCards: List<GiftCard> = emptyList()
    ): PaymentMethodSummary {
        val cashIncome = transactions
            .filter {
                it.type == TransactionType.INCOME &&
                (it.incomeType == IncomeType.CASH || it.incomeType == null) &&
                it.category != "상품권 환급" // 상품권 환급으로 인한 현금 수입 제외
            }
            .sumOf { it.amount }

        val cashExpense = transactions
            .filter { it.type == TransactionType.EXPENSE && (it.paymentMethod == PaymentMethod.CASH || it.paymentMethod == null) }
            .sumOf { it.amount }

        val cardExpense = transactions
            .filter { it.type == TransactionType.EXPENSE && it.paymentMethod == PaymentMethod.CARD }
            .sumOf { it.amount }

        // 더치페이 관련 분석
        val cardTransactions = transactions.filter { it.type == TransactionType.EXPENSE && it.paymentMethod == PaymentMethod.CARD }

        val cardActualExpense = cardTransactions
            .sumOf { transaction ->
                if (transaction.isSettlement && transaction.actualAmount != null) {
                    transaction.actualAmount
                } else {
                    transaction.amount
                }
            }

        val settlementIncome = transactions
            .filter { it.type == TransactionType.EXPENSE && it.isSettlement && it.settlementAmount != null }
            .sumOf { it.settlementAmount!! }

        // 잔액권 분석 (해당 기간 거래 + 잔액이 남아있는 모든 잔액권)
        val balanceCardNamesFromTransactions = transactions
            .filter { it.balanceCardId != null && it.cardName != null }
            .map { it.cardName!! }
            .toSet()

        val balanceCardNamesFromAvailable = availableBalanceCards
            .filter { it.isActive && it.currentBalance > 0 }
            .map { it.name }
            .toSet()

        val allBalanceCardNames = (balanceCardNamesFromTransactions + balanceCardNamesFromAvailable).distinct()

        val balanceCardSummaries = allBalanceCardNames.map { cardName ->
            val cardTransactions = transactions.filter { it.cardName == cardName && it.balanceCardId != null }

            val income = cardTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            val expense = cardTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val currentCard = availableBalanceCards.find { it.name == cardName }
            val currentBalance = currentCard?.currentBalance ?: 0.0

            BalanceCardSummary(
                id = currentCard?.id ?: cardTransactions.firstOrNull()?.balanceCardId ?: "",
                name = cardName,
                income = income,
                expense = expense,
                currentBalance = currentBalance
            )
        }.filter { it.currentBalance > 0 } // 잔액이 0인 것은 제외

        // 상품권 분석 (해당 기간 거래 + 잔액이 남아있는 모든 상품권)
        val giftCardNamesFromTransactions = transactions
            .filter { it.giftCardId != null && it.cardName != null }
            .map { it.cardName!! }
            .toSet()

        val giftCardNamesFromAvailable = availableGiftCards
            .filter { it.isActive && it.remainingAmount > 0 }
            .map { it.name }
            .toSet()

        val allGiftCardNames = (giftCardNamesFromTransactions + giftCardNamesFromAvailable).distinct()

        val giftCardSummaries = allGiftCardNames.map { cardName ->
            val cardTransactions = transactions.filter { it.cardName == cardName && it.giftCardId != null }

            val income = cardTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            val expense = cardTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val currentCard = availableGiftCards.find { it.name == cardName }
            val currentBalance = currentCard?.remainingAmount ?: 0.0

            GiftCardSummary(
                id = currentCard?.id ?: cardTransactions.firstOrNull()?.giftCardId ?: "",
                name = cardName,
                income = income,
                expense = expense,
                currentBalance = currentBalance
            )
        }.filter { it.currentBalance > 0 } // 잔액이 0인 것은 제외

        return PaymentMethodSummary(
            cashIncome = cashIncome,
            cashExpense = cashExpense,
            cardExpense = cardExpense,
            cardActualExpense = cardActualExpense,
            settlementIncome = settlementIncome,
            balanceCards = balanceCardSummaries,
            giftCards = giftCardSummaries
        )
    }
}

@Serializable
data class MonthlySummary(
    val year: Int,
    val month: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double
) {
    companion object {
        fun fromTransactions(year: Int, month: Int, transactions: List<Transaction>): MonthlySummary {
            val monthTransactions = transactions.filter { 
                it.date.year == year && it.date.monthNumber == month 
            }
            
            val income = monthTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
            
            val expense = monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            
            return MonthlySummary(
                year = year,
                month = month,
                totalIncome = income,
                totalExpense = expense,
                balance = income - expense
            )
        }
    }
}