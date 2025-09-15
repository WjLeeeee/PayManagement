package com.woojin.paymanagement.utils

import platform.Foundation.NSUserDefaults

actual class PreferencesManager {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    actual fun isFirstLaunch(): Boolean {
        return userDefaults.boolForKey("is_first_launch").takeIf { 
            userDefaults.objectForKey("is_first_launch") != null 
        } ?: true
    }
    
    actual fun setFirstLaunchCompleted() {
        userDefaults.setBool(false, forKey = "is_first_launch")
    }
    
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
    
    actual fun isTutorialCompleted(): Boolean {
        return userDefaults.boolForKey("tutorial_completed").takeIf { 
            userDefaults.objectForKey("tutorial_completed") != null 
        } ?: false
    }
    
    actual fun setTutorialCompleted() {
        userDefaults.setBool(true, forKey = "tutorial_completed")
    }
}