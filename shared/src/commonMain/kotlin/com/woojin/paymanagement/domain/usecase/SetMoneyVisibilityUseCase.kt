package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.PreferencesRepository

class SetMoneyVisibilityUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(visible: Boolean) {
        preferencesRepository.setMoneyVisible(visible)
    }
}