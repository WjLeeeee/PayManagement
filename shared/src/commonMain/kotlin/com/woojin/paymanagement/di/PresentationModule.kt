package com.woojin.paymanagement.di

import com.woojin.paymanagement.presentation.addtransaction.AddTransactionViewModel
import com.woojin.paymanagement.presentation.budgetsettings.BudgetSettingsViewModel
import com.woojin.paymanagement.presentation.calendar.CalendarViewModel
import com.woojin.paymanagement.presentation.cardmanagement.CardManagementViewModel
import com.woojin.paymanagement.presentation.categorymanagement.CategoryManagementViewModel
import com.woojin.paymanagement.presentation.datedetail.DateDetailViewModel
import com.woojin.paymanagement.presentation.monthlycomparison.MonthlyComparisonViewModel
import com.woojin.paymanagement.presentation.parsedtransaction.ParsedTransactionViewModel
import com.woojin.paymanagement.presentation.paydaysetup.PaydaySetupViewModel
import com.woojin.paymanagement.presentation.recurringtransaction.RecurringTransactionViewModel
import com.woojin.paymanagement.presentation.statistics.StatisticsViewModel
import com.woojin.paymanagement.presentation.tipdonation.TipDonationViewModel
import com.woojin.paymanagement.presentation.tutorial.CalendarTutorialViewModel
import org.koin.dsl.module

val presentationModule = module {
    // ViewModels
    factory {
        CalendarViewModel(
            preferencesRepository = get(),
            getPayPeriodSummaryUseCase = get(),
            getDailyTransactionsUseCase = get(),
            getMoneyVisibilityUseCase = get(),
            setMoneyVisibilityUseCase = get(),
            updateTransactionUseCase = get(),
            getCategoriesUseCase = get(),
            coroutineScope = get()
        )
    }
    factory { AddTransactionViewModel(get(), get(), get(), get(), get(), get()) }
    factory { DateDetailViewModel(get(), get(), get(), get(), get()) }
    factory { PaydaySetupViewModel(get(), get(), get()) }
    factory { StatisticsViewModel(get(), get(), get(), get(), get(), get()) }
    factory { CalendarTutorialViewModel(get()) }
    factory { ParsedTransactionViewModel(get(), get(), get()) }
    factory { CategoryManagementViewModel(get(), get(), get(), get()) }
    factory { CardManagementViewModel(get()) }
    factory { BudgetSettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { TipDonationViewModel(get(), get()) }
    factory { MonthlyComparisonViewModel(get(), get(), get(), get()) }
    factory { RecurringTransactionViewModel(get(), get(), get(), get(), get(), get()) }
}