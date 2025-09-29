package com.woojin.paymanagement.di

import com.woojin.paymanagement.presentation.addtransaction.AddTransactionViewModel
import com.woojin.paymanagement.presentation.app.AppViewModel
import com.woojin.paymanagement.presentation.calendar.CalendarViewModel
import com.woojin.paymanagement.presentation.datedetail.DateDetailViewModel
import com.woojin.paymanagement.presentation.paydaysetup.PaydaySetupViewModel
import com.woojin.paymanagement.presentation.statistics.StatisticsViewModel
import com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val presentationModule = module {
    // CoroutineScope for ViewModels
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    // ViewModels
    single { AppViewModel(get(), get(), get(), get(), get()) }
    factory { CalendarViewModel(get(), get(), get()) }
    single { AddTransactionViewModel(get(), get(), get(), get(), get()) }
    factory { DateDetailViewModel(get(), get(), get()) }
    factory { PaydaySetupViewModel(get(), get(), get()) }
    factory { StatisticsViewModel(get(), get(), get(), get()) }
    factory { CalendarTutorialViewModel(get()) }
}