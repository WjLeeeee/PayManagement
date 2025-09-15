package com.woojin.paymanagement.utils

expect class PreferencesManager {
    fun isFirstLaunch(): Boolean
    fun setFirstLaunchCompleted()
    
    fun getPayday(): Int // 1-31
    fun setPayday(day: Int)
    fun isPaydaySet(): Boolean
    
    fun getPaydayAdjustment(): PaydayAdjustment
    fun setPaydayAdjustment(adjustment: PaydayAdjustment)
    
    fun isTutorialCompleted(): Boolean
    fun setTutorialCompleted()
}

enum class PaydayAdjustment {
    BEFORE_WEEKEND, // 주말/공휴일 이전에 지급
    AFTER_WEEKEND   // 주말/공휴일 이후에 지급
}