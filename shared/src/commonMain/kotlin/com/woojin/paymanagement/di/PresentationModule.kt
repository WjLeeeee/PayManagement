package com.woojin.paymanagement.di

import com.woojin.paymanagement.presentation.addtransaction.AddTransactionViewModel
import com.woojin.paymanagement.presentation.calendar.CalendarViewModel
import com.woojin.paymanagement.presentation.datedetail.DateDetailViewModel
import com.woojin.paymanagement.presentation.paydaysetup.PaydaySetupViewModel
import com.woojin.paymanagement.presentation.statistics.StatisticsViewModel
import com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel
import com.woojin.paymanagement.presentation.parsedtransaction.ParsedTransactionViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val presentationModule = module {
    // ViewModels
    factory { CalendarViewModel(get(), get(), get(), get(), get()) }
    factory { AddTransactionViewModel(get(), get(), get(), get(), get()) }
    factory { DateDetailViewModel(get(), get(), get()) }
    factory { PaydaySetupViewModel(get(), get(), get()) }
    factory { StatisticsViewModel(get(), get(), get(), get()) }
    factory { CalendarTutorialViewModel(get()) }
    factory { ParsedTransactionViewModel(get(), get(), get(), get()) }
}