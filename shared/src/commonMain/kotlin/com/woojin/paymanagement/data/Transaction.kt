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
    val cardName: String? = null,        // 잔액권 이름 (CARD 결제면 카드 이름)
    val settlementAmount: Double? = null, // 정산받을 금액 (더치페이 시)
    val isSettlement: Boolean = false,   // 더치페이 여부
    val subCategory: String? = null      // 소분류
) {
    /**
     * 화면에 표시할 금액 (정산금액 제외)
     * 예: 50,000원 결제, 40,000원 정산받으면 → 10,000원 표시
     */
    val displayAmount: Double
        get() = if (isSettlement && settlementAmount != null) {
            amount - settlementAmount
        } else {
            amount
        }
}

@Serializable
enum class TransactionType {
    INCOME,
    EXPENSE,
    SAVING,
    INVESTMENT
}

// 투자 중 돌아온 돈으로 보는 카테고리 (나머지 투자는 나간 돈)
val INVESTMENT_INCOME_CATEGORIES = setOf("익절", "배당금")

@Serializable
enum class IncomeType {
    CASH,        // 현금
    BALANCE_CARD // 잔액권 (편의점 상품권 등, 잔액 관리 필요)
}

@Serializable
enum class PaymentMethod {
    CASH,        // 현금 지출 (체크카드 포함)
    CARD,        // 신용카드 지출
    BALANCE_CARD // 잔액권 사용
}

// 상품권 기능 제거 전에 저장된 "GIFT_CARD"는 현금으로 읽음 (DB·백업·공유방 공통)
private const val REMOVED_GIFT_CARD = "GIFT_CARD"
fun paymentMethodOf(name: String): PaymentMethod =
    if (name == REMOVED_GIFT_CARD) PaymentMethod.CASH else PaymentMethod.valueOf(name)
fun incomeTypeOf(name: String): IncomeType =
    if (name == REMOVED_GIFT_CARD) IncomeType.CASH else IncomeType.valueOf(name)

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
    val cardBreakdowns: List<CardBreakdown> = emptyList()
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
data class CustomPaymentMethod(
    val id: String,
    val name: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
)

@Serializable
data class CardBreakdown(
    val cardName: String?,  // null = 미지정
    val expense: Double
)

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
        availableBalanceCards: List<BalanceCard> = emptyList()
    ): PaymentMethodSummary {
        val cashIncome = transactions
            .filter {
                it.type == TransactionType.INCOME &&
                (it.incomeType == IncomeType.CASH || it.incomeType == null)
            }
            .sumOf { it.displayAmount }

        val cashExpense = transactions
            .filter { it.type == TransactionType.EXPENSE && (it.paymentMethod == PaymentMethod.CASH || it.paymentMethod == null) }
            .sumOf { it.displayAmount }

        val cardExpense = transactions
            .filter { it.type == TransactionType.EXPENSE && it.paymentMethod == PaymentMethod.CARD }
            .sumOf { it.displayAmount }

        // 더치페이 관련 분석
        val cardTransactions = transactions.filter { it.type == TransactionType.EXPENSE && it.paymentMethod == PaymentMethod.CARD }

        val cardActualExpense = cardTransactions
            .sumOf { transaction ->
                transaction.displayAmount
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
                .sumOf { it.displayAmount }

            val expense = cardTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.displayAmount }

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

        // 카드별 지출 분석
        val cardBreakdowns = if (cardExpense > 0) {
            cardTransactions
                .groupBy { it.cardName }
                .map { (name, txns) ->
                    CardBreakdown(
                        cardName = name,
                        expense = txns.sumOf { it.displayAmount }
                    )
                }
                .sortedByDescending { it.expense }
        } else {
            emptyList()
        }

        return PaymentMethodSummary(
            cashIncome = cashIncome,
            cashExpense = cashExpense,
            cardExpense = cardExpense,
            cardActualExpense = cardActualExpense,
            settlementIncome = settlementIncome,
            balanceCards = balanceCardSummaries,
            cardBreakdowns = cardBreakdowns
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
                .sumOf { it.displayAmount }

            val expense = monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.displayAmount }

            // SAVING, INVESTMENT 타입은 수입/지출 합계에서 제외
            
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