package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.model.PaydaySetup
import com.woojin.paymanagement.utils.PaydayAdjustment

class ValidatePaydaySetupUseCase {
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    operator fun invoke(payday: Int, adjustment: PaydayAdjustment): ValidationResult {
        return when {
            payday !in 1..31 -> ValidationResult(
                isValid = false,
                errorMessage = "월급날은 1일부터 31일 사이여야 합니다."
            )
            else -> ValidationResult(isValid = true)
        }
    }

    operator fun invoke(paydaySetup: PaydaySetup): ValidationResult {
        return invoke(paydaySetup.payday, paydaySetup.adjustment)
    }
}