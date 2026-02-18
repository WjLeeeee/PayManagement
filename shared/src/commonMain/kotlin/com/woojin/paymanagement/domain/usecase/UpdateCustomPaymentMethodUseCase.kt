package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.CustomPaymentMethod
import com.woojin.paymanagement.domain.repository.CustomPaymentMethodRepository
import kotlinx.coroutines.flow.first

class UpdateCustomPaymentMethodUseCase(
    private val repository: CustomPaymentMethodRepository
) {
    suspend operator fun invoke(oldMethod: CustomPaymentMethod, newMethod: CustomPaymentMethod) {
        val nameChanged = oldMethod.name != newMethod.name

        if (nameChanged) {
            val existing = repository.getAllCustomPaymentMethods().first()
            val hasDuplicate = existing.any { it.id != newMethod.id && it.name == newMethod.name }
            if (hasDuplicate) {
                throw IllegalArgumentException("이미 동일한 이름의 결제수단이 존재합니다: ${newMethod.name}")
            }
        }

        repository.updateCustomPaymentMethod(newMethod)

        if (nameChanged) {
            repository.updateTransactionsCardName(
                oldCardName = oldMethod.name,
                newCardName = newMethod.name
            )
        }
    }
}
