package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.model.PaydaySetup
import com.woojin.paymanagement.domain.repository.PreferencesRepository

class GetPaydaySetupUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): PaydaySetup {
        val payday = preferencesRepository.getPayday()
        val adjustment = preferencesRepository.getPaydayAdjustment()
        return PaydaySetup(payday, adjustment)
    }
}