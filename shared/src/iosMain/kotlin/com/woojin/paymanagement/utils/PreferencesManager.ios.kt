package com.woojin.paymanagement.utils

import platform.Foundation.NSUserDefaults

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
}