package com.woojin.paymanagement.data.repository

import com.woojin.paymanagement.data.CustomPaymentMethod
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.domain.repository.CustomPaymentMethodRepository
import kotlinx.coroutines.flow.Flow

class CustomPaymentMethodRepositoryImpl(
    private val databaseHelper: DatabaseHelper
) : CustomPaymentMethodRepository {

    override fun getAllCustomPaymentMethods(): Flow<List<CustomPaymentMethod>> {
        return databaseHelper.getAllCustomPaymentMethods()
    }

    override fun getAllCustomPaymentMethodsIncludingInactive(): Flow<List<CustomPaymentMethod>> {
        return databaseHelper.getAllCustomPaymentMethodsIncludingInactive()
    }

    override suspend fun insertCustomPaymentMethod(customPaymentMethod: CustomPaymentMethod) {
        databaseHelper.insertCustomPaymentMethod(customPaymentMethod)
    }

    override suspend fun updateCustomPaymentMethod(customPaymentMethod: CustomPaymentMethod) {
        databaseHelper.updateCustomPaymentMethod(customPaymentMethod)
    }

    override suspend fun deleteCustomPaymentMethod(id: String) {
        databaseHelper.deleteCustomPaymentMethod(id)
    }

    override suspend fun updateTransactionsCardName(oldCardName: String, newCardName: String) {
        databaseHelper.updateTransactionsCardName(oldCardName, newCardName)
    }

    override suspend fun clearAllDefaultPaymentMethods() {
        databaseHelper.clearAllDefaultPaymentMethods()
    }
}
