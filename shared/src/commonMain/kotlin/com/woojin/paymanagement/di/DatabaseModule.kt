package com.woojin.paymanagement.di

import com.woojin.paymanagement.database.DatabaseHelper
import com.woojin.paymanagement.database.PayManagementDatabase
import org.koin.dsl.module

val databaseModule = module {
    // DatabaseDriverFactory는 플랫폼별로 제공됨

    // PayManagementDatabase 싱글톤으로 제공
    single<PayManagementDatabase> {
        PayManagementDatabase(get()) // DatabaseDriverFactory.createDriver()를 주입받음
    }

    // DatabaseHelper 싱글톤으로 제공
    single<DatabaseHelper> {
        DatabaseHelper(get()) // PayManagementDatabase를 주입받음
    }
}