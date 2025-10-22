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