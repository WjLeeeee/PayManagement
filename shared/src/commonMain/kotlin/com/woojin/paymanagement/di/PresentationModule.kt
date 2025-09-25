package com.woojin.paymanagement.di

import com.woojin.paymanagement.presentation.addtransaction.AddTransactionViewModel
import com.woojin.paymanagement.presentation.calendar.CalendarViewModel
import com.woojin.paymanagement.presentation.datedetail.DateDetailViewModel
import com.woojin.paymanagement.presentation.statistics.StatisticsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val presentationModule = module {
    // ViewModels
    factory { CalendarViewModel(get(), get(), get()) }
    single { AddTransactionViewModel(get(), get(), get(), get(), get()) }
    factory { DateDetailViewModel(get(), get(), get()) }
    factory { StatisticsViewModel(get(), get(), get(), get()) }
}