package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.domain.repository.CustomPaymentMethodRepository

class DeleteCustomPaymentMethodUseCase(
    private val repository: CustomPaymentMethodRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteCustomPaymentMethod(id)
    }
}
