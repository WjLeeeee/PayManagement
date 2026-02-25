package com.woojin.paymanagement.utils

expect class PreferencesManager {
    fun getPayday(): Int // 1-31
    fun setPayday(day: Int)
    fun isPaydaySet(): Boolean

    fun getPaydayAdjustment(): PaydayAdjustment
    fun setPaydayAdjustment(adjustment: PaydayAdjustment)

    fun isCalendarTutorialCompleted(): Boolean
    fun setCalendarTutorialCompleted()

    fun isMoneyVisible(): Boolean
    fun setMoneyVisible(visible: Boolean)

    fun getThemeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)

    fun getMonthlySalary(): Double
    fun setMonthlySalary(salary: Double)

    // 광고 제거 관련
    fun getAdRemovalExpiryTime(): Long // 만료 시간 (밀리초 타임스탬프)
    fun setAdRemovalExpiryTime(expiryTime: Long)
    fun isAdRemovalActive(): Boolean // 현재 광고 제거 활성화 여부

    // 쿠폰 관련
    fun isCouponUsed(couponCode: String): Boolean
    fun markCouponAsUsed(couponCode: String)

    // 마지막으로 체크한 급여 기간 시작일
    fun getLastCheckedPayPeriodStartDate(): String? // "yyyy-MM-dd" 형식
    fun setLastCheckedPayPeriodStartDate(date: String)

    // 권한 안내 다이얼로그 표시 여부
    fun isPermissionGuideShown(): Boolean
    fun setPermissionGuideShown()

    // 언어 설정
    fun getSystemLanguageCode(): String // 시스템 로케일에서 언어 코드 가져오기
    fun getLanguageCode(): String // "ko" or "en"
    fun setLanguageCode(code: String)

    // 반복 거래 자동 실행
    fun isRecurringAutoExecuteEnabled(): Boolean
    fun setRecurringAutoExecuteEnabled(enabled: Boolean)
}

enum class PaydayAdjustment {
    BEFORE_WEEKEND, // 주말/공휴일 이전에 지급
    AFTER_WEEKEND   // 주말/공휴일 이후에 지급
}

enum class ThemeMode {
    SYSTEM,  // 시스템 설정 따름
    LIGHT,   // 항상 라이트
    DARK     // 항상 다크
}