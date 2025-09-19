package com.woojin.paymanagement.di

import com.woojin.paymanagement.presentation.calendar.CalendarViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val presentationModule = module {
    // ViewModels
    factory { CalendarViewModel(get(), get(), get()) }
}