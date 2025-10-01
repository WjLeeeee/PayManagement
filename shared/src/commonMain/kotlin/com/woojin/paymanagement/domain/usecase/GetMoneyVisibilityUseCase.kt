package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.PreferencesRepository

class GetMoneyVisibilityUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Boolean {
        return preferencesRepository.isMoneyVisible()
    }
}