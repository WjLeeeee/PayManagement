package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.utils.PaydayAdjustment

interface PreferencesRepository {
    fun getPayday(): Int
    fun getPaydayAdjustment(): PaydayAdjustment
    fun setPayday(day: Int)
    fun setPaydayAdjustment(adjustment: PaydayAdjustment)
    fun isMoneyVisible(): Boolean
    fun setMoneyVisible(visible: Boolean)
    fun getMonthlySalary(): Double
    fun setMonthlySalary(salary: Double)
}