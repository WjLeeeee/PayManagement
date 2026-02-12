package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.CustomPaymentMethod
import com.woojin.paymanagement.domain.repository.CustomPaymentMethodRepository

class AddCustomPaymentMethodUseCase(
    private val repository: CustomPaymentMethodRepository
) {
    suspend operator fun invoke(customPaymentMethod: CustomPaymentMethod) {
        repository.insertCustomPaymentMethod(customPaymentMethod)
    }
}
