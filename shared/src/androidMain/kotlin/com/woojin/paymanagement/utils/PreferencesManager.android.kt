package com.woojin.paymanagement.utils

import android.content.Context
import android.content.SharedPreferences

actual class PreferencesManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pay_management_prefs", Context.MODE_PRIVATE)
    
    actual fun getPayday(): Int {
        return prefs.getInt("payday", 25) // 기본값 25일
    }
    
    actual fun setPayday(day: Int) {
        prefs.edit().putInt("payday", day).apply()
    }
    
    actual fun isPaydaySet(): Boolean {
        return prefs.contains("payday")
    }
    
    actual fun getPaydayAdjustment(): PaydayAdjustment {
        val adjustment = prefs.getString("payday_adjustment", PaydayAdjustment.BEFORE_WEEKEND.name)
        return PaydayAdjustment.valueOf(adjustment ?: PaydayAdjustment.BEFORE_WEEKEND.name)
    }
    
    actual fun setPaydayAdjustment(adjustment: PaydayAdjustment) {
        prefs.edit().putString("payday_adjustment", adjustment.name).apply()
    }
    
    actual fun isCalendarTutorialCompleted(): Boolean {
        return prefs.getBoolean("calendar_tutorial_completed", false)
    }

    actual fun setCalendarTutorialCompleted() {
        prefs.edit().putBoolean("calendar_tutorial_completed", true).apply()
    }

    actual fun isMoneyVisible(): Boolean {
        return prefs.getBoolean("money_visible", true) // 기본값 true (보임)
    }

    actual fun setMoneyVisible(visible: Boolean) {
        prefs.edit().putBoolean("money_visible", visible).apply()
    }

    actual fun getThemeMode(): ThemeMode {
        val mode = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return ThemeMode.valueOf(mode ?: ThemeMode.SYSTEM.name)
    }

    actual fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    actual fun getMonthlySalary(): Double {
        return prefs.getFloat("monthly_salary", 0f).toDouble()
    }

    actual fun setMonthlySalary(salary: Double) {
        prefs.edit().putFloat("monthly_salary", salary.toFloat()).apply()
    }
}