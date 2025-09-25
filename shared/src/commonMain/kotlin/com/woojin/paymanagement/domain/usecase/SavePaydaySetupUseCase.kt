package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.model.PaydaySetup
import com.woojin.paymanagement.domain.repository.PreferencesRepository

class SavePaydaySetupUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(paydaySetup: PaydaySetup) {
        preferencesRepository.setPayday(paydaySetup.payday)
        preferencesRepository.setPaydayAdjustment(paydaySetup.adjustment)
    }
}