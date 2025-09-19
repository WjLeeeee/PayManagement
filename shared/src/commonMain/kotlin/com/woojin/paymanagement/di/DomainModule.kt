package com.woojin.paymanagement.di

import com.woojin.paymanagement.data.repository.TransactionRepositoryImpl
import com.woojin.paymanagement.domain.repository.TransactionRepository
import com.woojin.paymanagement.domain.usecase.GetDailyTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.GetPayPeriodSummaryUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val domainModule = module {

    // Repository
    singleOf(::TransactionRepositoryImpl) bind TransactionRepository::class
    single<com.woojin.paymanagement.domain.repository.PreferencesRepository> {
        com.woojin.paymanagement.data.repository.PreferencesRepositoryImpl(get())
    }

    // Use Cases
    factory { GetPayPeriodSummaryUseCase() }
    factory { GetDailyTransactionsUseCase() }
}