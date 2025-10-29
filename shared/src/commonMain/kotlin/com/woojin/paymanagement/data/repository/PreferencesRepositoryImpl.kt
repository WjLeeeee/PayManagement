package com.woojin.paymanagement.data.repository

import com.woojin.paymanagement.domain.repository.PreferencesRepository
import com.woojin.paymanagement.utils.PaydayAdjustment
import com.woojin.paymanagement.utils.PreferencesManager

class PreferencesRepositoryImpl(
    private val preferencesManager: PreferencesManager
) : PreferencesRepository {

    override fun getPayday(): Int {
        return preferencesManager.getPayday()
    }

    override fun getPaydayAdjustment(): PaydayAdjustment {
        return preferencesManager.getPaydayAdjustment()
    }

    override fun setPayday(day: Int) {
        preferencesManager.setPayday(day)
    }

    override fun setPaydayAdjustment(adjustment: PaydayAdjustment) {
        preferencesManager.setPaydayAdjustment(adjustment)
    }

    override fun isMoneyVisible(): Boolean {
        return preferencesManager.isMoneyVisible()
    }

    override fun setMoneyVisible(visible: Boolean) {
        preferencesManager.setMoneyVisible(visible)
    }

    override fun getMonthlySalary(): Double {
        return preferencesManager.getMonthlySalary()
    }

    override fun setMonthlySalary(salary: Double) {
        preferencesManager.setMonthlySalary(salary)
    }
}