package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.CustomPaymentMethod
import com.woojin.paymanagement.domain.repository.CustomPaymentMethodRepository
import kotlinx.coroutines.flow.Flow

class GetCustomPaymentMethodsUseCase(
    private val repository: CustomPaymentMethodRepository
) {
    operator fun invoke(): Flow<List<CustomPaymentMethod>> {
        return repository.getAllCustomPaymentMethods()
    }
}
