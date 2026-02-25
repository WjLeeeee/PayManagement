package com.woojin.paymanagement.utils

import platform.Foundation.NSUserDefaults
import kotlinx.datetime.Clock

actual class PreferencesManager {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual fun getPayday(): Int {
        return userDefaults.integerForKey("payday").takeIf {
            userDefaults.objectForKey("payday") != null
        }?.toInt() ?: 25 // 기본값 25일
    }

    actual fun setPayday(day: Int) {
        userDefaults.setInteger(day.toLong(), forKey = "payday")
    }

    actual fun isPaydaySet(): Boolean {
        return userDefaults.objectForKey("payday") != null
    }

    actual fun getPaydayAdjustment(): PaydayAdjustment {
        val adjustment = userDefaults.stringForKey("payday_adjustment") ?: PaydayAdjustment.BEFORE_WEEKEND.name
        return try {
            PaydayAdjustment.valueOf(adjustment)
        } catch (e: IllegalArgumentException) {
            PaydayAdjustment.BEFORE_WEEKEND
        }
    }

    actual fun setPaydayAdjustment(adjustment: PaydayAdjustment) {
        userDefaults.setObject(adjustment.name, forKey = "payday_adjustment")
    }

    actual fun isCalendarTutorialCompleted(): Boolean {
        return userDefaults.boolForKey("calendar_tutorial_completed").takeIf {
            userDefaults.objectForKey("calendar_tutorial_completed") != null
        } ?: false
    }

    actual fun setCalendarTutorialCompleted() {
        userDefaults.setBool(true, forKey = "calendar_tutorial_completed")
    }

    actual fun isMoneyVisible(): Boolean {
        return userDefaults.boolForKey("money_visible").takeIf {
            userDefaults.objectForKey("money_visible") != null
        } ?: true // 기본값 true (보임)
    }

    actual fun setMoneyVisible(visible: Boolean) {
        userDefaults.setBool(visible, forKey = "money_visible")
    }

    actual fun getThemeMode(): ThemeMode {
        val mode = userDefaults.stringForKey("theme_mode") ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(mode)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    actual fun setThemeMode(mode: ThemeMode) {
        userDefaults.setObject(mode.name, forKey = "theme_mode")
    }

    actual fun getMonthlySalary(): Double {
        return userDefaults.doubleForKey("monthly_salary").takeIf {
            userDefaults.objectForKey("monthly_salary") != null
        } ?: 0.0
    }

    actual fun setMonthlySalary(salary: Double) {
        userDefaults.setDouble(salary, forKey = "monthly_salary")
    }

    // 광고 제거 관련
    actual fun getAdRemovalExpiryTime(): Long {
        val stringValue = userDefaults.stringForKey("ad_removal_expiry_time")
        return stringValue?.toLongOrNull() ?: 0L
    }

    actual fun setAdRemovalExpiryTime(expiryTime: Long) {
        userDefaults.setObject(expiryTime.toString(), forKey = "ad_removal_expiry_time")
    }

    actual fun isAdRemovalActive(): Boolean {
        val expiryTime = getAdRemovalExpiryTime()
        return expiryTime > Clock.System.now().toEpochMilliseconds()
    }

    // 쿠폰 관련
    actual fun isCouponUsed(couponCode: String): Boolean {
        return userDefaults.boolForKey("coupon_used_$couponCode").takeIf {
            userDefaults.objectForKey("coupon_used_$couponCode") != null
        } ?: false
    }

    actual fun markCouponAsUsed(couponCode: String) {
        userDefaults.setBool(true, forKey = "coupon_used_$couponCode")
    }

    // 마지막으로 체크한 급여 기간 시작일
    actual fun getLastCheckedPayPeriodStartDate(): String? {
        return userDefaults.stringForKey("last_checked_pay_period_start_date")
    }

    actual fun setLastCheckedPayPeriodStartDate(date: String) {
        userDefaults.setObject(date, forKey = "last_checked_pay_period_start_date")
    }

    // 권한 안내 다이얼로그 표시 여부
    actual fun isPermissionGuideShown(): Boolean {
        return userDefaults.boolForKey("permission_guide_shown").takeIf {
            userDefaults.objectForKey("permission_guide_shown") != null
        } ?: false
    }

    actual fun setPermissionGuideShown() {
        userDefaults.setBool(true, forKey = "permission_guide_shown")
    }

    // 언어 설정
    actual fun getSystemLanguageCode(): String {
        val locale = platform.Foundation.NSLocale.preferredLanguages.firstOrNull() as? String ?: "ko"
        val lang = locale.substringBefore("-").substringBefore("_")
        return if (lang == "ko" || lang == "en") lang else "ko"
    }

    actual fun getLanguageCode(): String {
        return userDefaults.stringForKey("language_code") ?: getSystemLanguageCode()
    }

    actual fun setLanguageCode(code: String) {
        userDefaults.setObject(code, forKey = "language_code")
    }

    // 반복 거래 자동 실행
    actual fun isRecurringAutoExecuteEnabled(): Boolean {
        return userDefaults.boolForKey("recurring_auto_execute").takeIf {
            userDefaults.objectForKey("recurring_auto_execute") != null
        } ?: false
    }

    actual fun setRecurringAutoExecuteEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, forKey = "recurring_auto_execute")
    }
}
