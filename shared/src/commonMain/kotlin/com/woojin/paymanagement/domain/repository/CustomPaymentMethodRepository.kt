package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.data.CustomPaymentMethod
import kotlinx.coroutines.flow.Flow

interface CustomPaymentMethodRepository {
    fun getAllCustomPaymentMethods(): Flow<List<CustomPaymentMethod>>
    fun getAllCustomPaymentMethodsIncludingInactive(): Flow<List<CustomPaymentMethod>>
    suspend fun insertCustomPaymentMethod(customPaymentMethod: CustomPaymentMethod)
    suspend fun updateCustomPaymentMethod(customPaymentMethod: CustomPaymentMethod)
    suspend fun deleteCustomPaymentMethod(id: String)
    suspend fun updateTransactionsCardName(oldCardName: String, newCardName: String)
    suspend fun clearAllDefaultPaymentMethods()
}
