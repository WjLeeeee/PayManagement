package com.woojin.paymanagement.di

import com.woojin.paymanagement.database.DatabaseDriverFactory
import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.database.PayManagementDatabase
import com.woojin.paymanagement.data.repository.ParsedTransactionRepositoryImpl
import com.woojin.paymanagement.domain.repository.ParsedTransactionRepository
import com.woojin.paymanagement.data.repository.TransactionRepositoryImpl
import com.woojin.paymanagement.domain.repository.TransactionRepository
import org.koin.dsl.module

val databaseModule = module {
    // DatabaseDriverFactory는 플랫폼별로 제공됨

    // PayManagementDatabase 싱글톤으로 제공
    single<PayManagementDatabase> {
        val databaseDriverFactory = get<DatabaseDriverFactory>()
        PayManagementDatabase(databaseDriverFactory.createDriver())
    }

    // DatabaseHelper 싱글톤으로 제공
    single<DatabaseHelper> {
        DatabaseHelper(get()) // PayManagementDatabase를 주입받음
    }

    // Repositories
    single<TransactionRepository> {
        TransactionRepositoryImpl(get())
    }

    single<ParsedTransactionRepository> {
        ParsedTransactionRepositoryImpl(get())
    }
}