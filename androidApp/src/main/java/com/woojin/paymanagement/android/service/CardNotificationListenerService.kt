package com.woojin.paymanagement.android.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.woojin.paymanagement.android.util.TransactionNotificationHelper
import com.woojin.paymanagement.data.ParsedTransaction
import com.woojin.paymanagement.domain.usecase.InsertParsedTransactionUseCase
import com.woojin.paymanagement.domain.repository.ParsedTransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.security.MessageDigest
import java.util.Calendar

/**
 * 카드 알림을 감지하고 파싱하는 NotificationListenerService
 */
class CardNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "CardNotificationListener"
        private const val SHINHAN_CARD_PACKAGE = "com.shcard.smartpay" // 신한카드 앱 패키지명
        private const val SAMSUNG_PAY_PACKAGE = "com.samsung.android.spay" // 삼성페이 앱 패키지명
        private const val DUPLICATE_CHECK_WINDOW_MS = 1 * 60 * 1000L // 1분

        /**
         * 알림 텍스트의 SHA-256 해시를 생성하여 고유 ID로 사용
         * 같은 알림 내용은 같은 ID를 가지므로 중복 저장 방지
         */
        private fun generateNotificationId(rawNotification: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(rawNotification.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    private val insertParsedTransactionUseCase: InsertParsedTransactionUseCase by inject()
    private val parsedTransactionRepository: ParsedTransactionRepository by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        TransactionNotificationHelper.initialize(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let {
            when (it.packageName) {
                SHINHAN_CARD_PACKAGE -> handleCardNotification(it)
                SAMSUNG_PAY_PACKAGE -> handleSamsungPayNotification(it)
            }
        }
    }

    private fun handleCardNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras

            // 알림 제목과 내용 추출
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text

            Log.d(TAG, "Notification received - Title: $title")
            Log.d(TAG, "Notification received - Text: $bigText")

            // 승인 알림인지 확인 (일반 승인 또는 자동납부 승인)
            if (title.contains("신한카드") &&
                (bigText.contains("승인") || title.contains("자동납부"))) {
                val parsedTransaction = parseNotification(bigText, title)

                parsedTransaction?.let { transaction ->
                    Log.d(TAG, "Parsed transaction: $transaction")
                    saveParsedTransaction(transaction)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification", e)
        }
    }

    /**
     * 신한카드 알림 텍스트를 파싱하여 ParsedTransaction 생성
     *
     * 예시 포맷 1 (일반 승인):
     * [신한카드(9686)승인] 이*진
     * -승인금액: 7,818원(일시불)
     * -승인일시: 10/02 14:20
     * -가맹점명: (주)비바리퍼블리카
     * -누적금액: 205,032원
     *
     * 예시 포맷 2 (자동납부):
     * [신한카드 자동납부 정상승인] 이우진님
     * -승인일자: 10/10
     * -승인금액: 1,020원(일시불)
     * -가맹점명: 서울도시가스(주)
     */
    private fun parseNotification(text: String, title: String): ParsedTransaction? {
        try {
            // 승인금액 파싱: "승인금액: 7,818원(일시불)" -> 7818.0
            val amountRegex = """승인금액:\s*([\d,]+)원""".toRegex()
            val amountMatch = amountRegex.find(text)
            val amount = amountMatch?.groupValues?.get(1)
                ?.replace(",", "")
                ?.toDoubleOrNull()

            var month: Int? = null
            var day: Int? = null

            // 승인일시 파싱 (일반 승인): "승인일시: 10/02 14:20" -> LocalDate
            val dateTimeRegex = """승인일시:\s*(\d{1,2})/(\d{1,2})\s+\d{1,2}:\d{1,2}""".toRegex()
            val dateTimeMatch = dateTimeRegex.find(text)

            if (dateTimeMatch != null) {
                // 일반 승인 포맷
                month = dateTimeMatch.groupValues[1].toIntOrNull()
                day = dateTimeMatch.groupValues[2].toIntOrNull()
            } else {
                // 승인일자 파싱 (자동납부): "승인일자: 10/10" -> LocalDate
                val dateOnlyRegex = """승인일자:\s*(\d{1,2})/(\d{1,2})""".toRegex()
                val dateOnlyMatch = dateOnlyRegex.find(text)

                if (dateOnlyMatch != null) {
                    month = dateOnlyMatch.groupValues[1].toIntOrNull()
                    day = dateOnlyMatch.groupValues[2].toIntOrNull()
                }
            }

            // 가맹점명 파싱: "가맹점명: (주)비바리퍼블리카" -> (주)비바리퍼블리카
            val merchantRegex = """가맹점명:\s*(.+?)(?:\n|$)""".toRegex()
            val merchantMatch = merchantRegex.find(text)
            val merchantName = merchantMatch?.groupValues?.get(1)?.trim()

            // 모든 필수 값이 있는지 확인
            if (amount == null || month == null || day == null || merchantName.isNullOrBlank()) {
                Log.w(TAG, "Failed to parse notification - missing required fields")
                Log.w(TAG, "amount=$amount, month=$month, day=$day, merchantName=$merchantName")
                return null
            }

            // 현재 년도 가져오기
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)

            // LocalDate 생성
            val transactionDate = try {
                kotlinx.datetime.LocalDate(currentYear, month, day)
            } catch (e: Exception) {
                Log.e(TAG, "Invalid date: $currentYear/$month/$day", e)
                return null
            }

            return ParsedTransaction(
                id = generateNotificationId(text),  // 알림 내용의 해시를 ID로 사용
                amount = amount,
                merchantName = merchantName,
                date = transactionDate,
                rawNotification = text,
                isProcessed = false,
                createdAt = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notification", e)
            return null
        }
    }

    /**
     * 삼성페이 알림 처리
     *
     * 예시 포맷:
     * Title: Samsung Wallet   오후 6:39
     * Text: ￦950 결제완료
     *       (주)세계로데일리웨이
     */
    private fun handleSamsungPayNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text

            Log.d(TAG, "Samsung Pay Notification - Title: $title")
            Log.d(TAG, "Samsung Pay Notification - Text: $bigText")

            // Samsung Wallet 알림이고 ￦(금액) 정보가 있는 경우만 처리
            if (title.contains("Samsung Wallet", ignoreCase = true) && bigText.contains("￦")) {
                val parsedTransaction = parseSamsungPayNotification(bigText, title)

                parsedTransaction?.let { transaction ->
                    Log.d(TAG, "Parsed Samsung Pay transaction: $transaction")
                    saveParsedTransaction(transaction)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Samsung Pay notification", e)
        }
    }

    /**
     * 삼성페이 알림 파싱
     *
     * 예시: "￦950 결제완료\n(주)세계로데일리웨이"
     */
    private fun parseSamsungPayNotification(text: String, title: String): ParsedTransaction? {
        try {
            val lines = text.split("\n")

            // 첫 번째 줄에서 금액 파싱: "￦8,700 결제완료" 또는 "￦950 결제완료" -> 8700.0 또는 950.0
            val amountLine = lines.firstOrNull() ?: return null
            val amountRegex = """￦([\d,]+)""".toRegex()
            val amountMatch = amountRegex.find(amountLine)
            val amount = amountMatch?.groupValues?.get(1)
                ?.replace(",", "")
                ?.toDoubleOrNull() ?: return null

            // 두 번째 줄에서 가맹점명 파싱
            val merchantName = lines.getOrNull(1)?.trim()
            if (merchantName.isNullOrBlank()) {
                Log.w(TAG, "Failed to parse Samsung Pay notification - missing merchant name")
                return null
            }

            // 현재 날짜 사용 (삼성페이 알림에는 날짜 정보가 없음)
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

            val transactionDate = try {
                kotlinx.datetime.LocalDate(currentYear, currentMonth, currentDay)
            } catch (e: Exception) {
                Log.e(TAG, "Invalid date: $currentYear/$currentMonth/$currentDay", e)
                return null
            }

            return ParsedTransaction(
                id = generateNotificationId(text),
                amount = amount,
                merchantName = merchantName,
                date = transactionDate,
                rawNotification = text,
                isProcessed = false,
                createdAt = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Samsung Pay notification", e)
            return null
        }
    }

    /**
     * 파싱된 거래를 저장
     * 중복 체크: 최근 5분 이내 같은 금액의 거래가 있으면 저장하지 않음
     */
    private fun saveParsedTransaction(transaction: ParsedTransaction) {
        serviceScope.launch {
            try {
                // 중복 체크: 최근 5분 이내 같은 금액의 거래가 있는지 확인
                val currentTime = System.currentTimeMillis()
                val startTime = currentTime - DUPLICATE_CHECK_WINDOW_MS
                val isDuplicate = parsedTransactionRepository.hasRecentTransactionWithAmount(
                    amount = transaction.amount,
                    startTime = startTime,
                    endTime = currentTime
                )

                if (isDuplicate) {
                    Log.d(TAG, "Duplicate transaction detected (same amount within 5 minutes): ${transaction.merchantName} - ${transaction.amount}원")
                    Log.d(TAG, "Skipping save to prevent duplicate entries")
                    return@launch
                }

                // 중복이 아니면 저장
                insertParsedTransactionUseCase(transaction)
                Log.d(TAG, "Parsed transaction saved: ${transaction.merchantName} - ${transaction.amount}원 (ID: ${transaction.id.take(8)}...)")

                // 파싱 성공 시 사용자에게 알림 전송
                TransactionNotificationHelper.sendTransactionNotification(this@CardNotificationListenerService, transaction)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save parsed transaction", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}