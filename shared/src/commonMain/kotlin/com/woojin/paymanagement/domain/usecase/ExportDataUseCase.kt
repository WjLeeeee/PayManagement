package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.BackupData
import com.woojin.paymanagement.data.BalanceCardBackup
import com.woojin.paymanagement.data.GiftCardBackup
import com.woojin.paymanagement.data.TransactionBackup
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.utils.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 데이터를 JSON 형식으로 내보내는 UseCase
 */
class ExportDataUseCase(
    private val databaseHelper: DatabaseHelper,
    private val preferencesManager: PreferencesManager
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend operator fun invoke(): Result<String> {
        return try {
            // 모든 데이터 수집
            val transactions = databaseHelper.getAllTransactions().first()
            val balanceCards = databaseHelper.getAllBalanceCards().first()
            val giftCards = databaseHelper.getAllGiftCards().first()

            // 백업 데이터 생성
            val backupData = BackupData(
                version = 2,
                exportDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
                payday = preferencesManager.getPayday(),
                paydayAdjustment = preferencesManager.getPaydayAdjustment().name,
                transactions = transactions.map { it.toBackup() },
                balanceCards = balanceCards.map { it.toBackup() },
                giftCards = giftCards.map { it.toBackup() }
            )

            // JSON 변환
            val jsonString = json.encodeToString(backupData)
            Result.success(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun com.woojin.paymanagement.data.Transaction.toBackup() = TransactionBackup(
        id = id,
        date = date.toString(),
        amount = amount,
        type = type.name,
        category = category,
        memo = memo,
        paymentMethod = paymentMethod?.name,
        incomeType = incomeType?.name,
        balanceCardId = balanceCardId,
        giftCardId = giftCardId,
        cardName = cardName,
        merchant = merchant,
        actualAmount = actualAmount,
        settlementAmount = settlementAmount,
        isSettlement = isSettlement
    )

    private fun com.woojin.paymanagement.data.BalanceCard.toBackup() = BalanceCardBackup(
        id = id,
        name = name,
        initialAmount = initialAmount,
        currentBalance = currentBalance,
        createdDate = createdDate.toString(),
        isActive = isActive
    )

    private fun com.woojin.paymanagement.data.GiftCard.toBackup() = GiftCardBackup(
        id = id,
        name = name,
        totalAmount = totalAmount,
        usedAmount = usedAmount,
        createdDate = createdDate.toString(),
        isActive = isActive
    )
}
