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
        private const val DUPLICATE_CHECK_WINDOW_MS = 5 * 1000L // 5초

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
            // 취소 알림은 제외 (title에 "취소"가 포함된 경우)
            if (title.contains("신한카드") &&
                !title.contains("취소") &&
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
     * 예시 포맷 (2024년 이후):
     * Title: ₩1,700 결제 완료
     * Text: (주)카카오
     */
    private fun handleSamsungPayNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            Log.d(TAG, "Samsung Pay Notification - Title: $title")
            Log.d(TAG, "Samsung Pay Notification - Text: $text")

            // 새로운 형식: title에 "₩금액 결제 완료"가 있는 경우
            // ₩ (U+20A9) 또는 ￦ (U+FFE6) 둘 다 처리
            if (title.contains("결제 완료") && (title.contains("₩") || title.contains("￦"))) {
                val parsedTransaction = parseSamsungPayNotification(title, text)

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
     * 새로운 형식:
     * title: "₩1,700 결제 완료"
     * text: "(주)카카오"
     */
    private fun parseSamsungPayNotification(title: String, text: String): ParsedTransaction? {
        try {
            // title에서 금액 파싱: "₩1,700 결제 완료" -> 1700.0
            // ₩ (U+20A9) 또는 ￦ (U+FFE6) 둘 다 지원
            val amountRegex = """[₩￦]([\d,]+)""".toRegex()
            val amountMatch = amountRegex.find(title)
            val amount = amountMatch?.groupValues?.get(1)
                ?.replace(",", "")
                ?.toDoubleOrNull()

            if (amount == null) {
                Log.w(TAG, "Failed to parse Samsung Pay notification - missing amount")
                return null
            }

            // text에서 가맹점명 가져오기
            val merchantName = text.trim()
            if (merchantName.isBlank()) {
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

            // 고유 ID 생성 (title + text 조합)
            val rawNotification = "Samsung Pay: $title - $text"

            return ParsedTransaction(
                id = generateNotificationId(rawNotification),
                amount = amount,
                merchantName = merchantName,
                date = transactionDate,
                rawNotification = rawNotification,
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
     * 중복 체크: 최근 5초 이내 같은 금액의 거래가 있으면 저장하지 않음
     */
    private fun saveParsedTransaction(transaction: ParsedTransaction) {
        serviceScope.launch {
            try {
                // 중복 체크: 최근 5초 이내 같은 금액의 거래가 있는지 확인
                val currentTime = System.currentTimeMillis()
                val startTime = currentTime - DUPLICATE_CHECK_WINDOW_MS
                val isDuplicate = parsedTransactionRepository.hasRecentTransactionWithAmount(
                    amount = transaction.amount,
                    startTime = startTime,
                    endTime = currentTime
                )

                if (isDuplicate) {
                    Log.d(TAG, "Duplicate transaction detected (same amount within 5 seconds): ${transaction.merchantName} - ${transaction.amount}원")
                    Log.d(TAG, "Skipping save to prevent duplicate entries")
                    return@launch
                }

                // 저장 시도 및 결과 확인
                val wasInserted = insertParsedTransactionUseCase(transaction)

                if (wasInserted) {
                    // 실제로 DB에 저장되었을 때만 로그 및 알림 전송
                    Log.d(TAG, "Parsed transaction saved: ${transaction.merchantName} - ${transaction.amount}원 (ID: ${transaction.id.take(8)}...)")
                    TransactionNotificationHelper.sendTransactionNotification(this@CardNotificationListenerService, transaction)
                } else {
                    // 이미 존재하는 거래 (중복 ID)
                    Log.d(TAG, "Duplicate transaction ID detected: ${transaction.merchantName} - ${transaction.amount}원 (ID: ${transaction.id.take(8)}...)")
                    Log.d(TAG, "Skipping notification to prevent duplicate push")
                }
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