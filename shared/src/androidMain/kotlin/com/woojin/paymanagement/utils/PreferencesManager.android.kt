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
}