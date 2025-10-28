package com.woojin.paymanagement.di

import com.woojin.paymanagement.data.repository.TransactionRepositoryImpl
import com.woojin.paymanagement.data.repository.CategoryRepositoryImpl
import com.woojin.paymanagement.domain.repository.TransactionRepository
import com.woojin.paymanagement.domain.repository.CategoryRepository
import com.woojin.paymanagement.domain.usecase.CalculateDailySummaryUseCase
import com.woojin.paymanagement.domain.usecase.DeleteTransactionUseCase
import com.woojin.paymanagement.domain.usecase.GetAvailableBalanceCardsUseCase
import com.woojin.paymanagement.domain.usecase.GetAvailableGiftCardsUseCase
import com.woojin.paymanagement.domain.usecase.GetDailyTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.GetPayPeriodSummaryUseCase
import com.woojin.paymanagement.domain.usecase.GetTransactionsByDateUseCase
import com.woojin.paymanagement.domain.usecase.SaveMultipleTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.SaveTransactionUseCase
import com.woojin.paymanagement.domain.usecase.UpdateTransactionUseCase
import com.woojin.paymanagement.domain.usecase.GetPayPeriodTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.CalculateChartDataUseCase
import com.woojin.paymanagement.domain.usecase.AnalyzePaymentMethodsUseCase
import com.woojin.paymanagement.domain.usecase.GetPaydaySetupUseCase
import com.woojin.paymanagement.domain.usecase.SavePaydaySetupUseCase
import com.woojin.paymanagement.domain.usecase.ValidatePaydaySetupUseCase
import com.woojin.paymanagement.domain.usecase.GetMoneyVisibilityUseCase
import com.woojin.paymanagement.domain.usecase.SetMoneyVisibilityUseCase
import com.woojin.paymanagement.domain.usecase.GetUnprocessedParsedTransactionsUseCase
import com.woojin.paymanagement.domain.usecase.InsertParsedTransactionUseCase
import com.woojin.paymanagement.domain.usecase.MarkParsedTransactionProcessedUseCase
import com.woojin.paymanagement.domain.usecase.DeleteParsedTransactionUseCase
import com.woojin.paymanagement.domain.usecase.ExportDataUseCase
import com.woojin.paymanagement.domain.usecase.ImportDataUseCase
import com.woojin.paymanagement.domain.usecase.GetCategoriesUseCase
import com.woojin.paymanagement.domain.usecase.AddCategoryUseCase
import com.woojin.paymanagement.domain.usecase.DeleteCategoryUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val domainModule = module {

    // Repository
    singleOf(::TransactionRepositoryImpl) bind TransactionRepository::class
    singleOf(::CategoryRepositoryImpl) bind CategoryRepository::class
    single<com.woojin.paymanagement.domain.repository.PreferencesRepository> {
        com.woojin.paymanagement.data.repository.PreferencesRepositoryImpl(get())
    }

    // Use Cases
    factory { GetPayPeriodSummaryUseCase() }
    factory { GetDailyTransactionsUseCase() }
    factory { CalculateDailySummaryUseCase() }

    // AddTransaction Use Cases
    factoryOf(::SaveTransactionUseCase)
    factoryOf(::SaveMultipleTransactionsUseCase)
    factoryOf(::UpdateTransactionUseCase)
    factoryOf(::GetAvailableBalanceCardsUseCase)
    factoryOf(::GetAvailableGiftCardsUseCase)

    // DateDetail Use Cases
    factoryOf(::GetTransactionsByDateUseCase)
    factoryOf(::DeleteTransactionUseCase)

    // Statistics Use Cases
    factoryOf(::GetPayPeriodTransactionsUseCase)
    factoryOf(::CalculateChartDataUseCase)
    factoryOf(::AnalyzePaymentMethodsUseCase)

    // PaydaySetup Use Cases
    factoryOf(::GetPaydaySetupUseCase)
    factoryOf(::SavePaydaySetupUseCase)
    factoryOf(::ValidatePaydaySetupUseCase)

    // Money Visibility Use Cases
    factoryOf(::GetMoneyVisibilityUseCase)
    factoryOf(::SetMoneyVisibilityUseCase)

    // ParsedTransaction Use Cases
    factoryOf(::GetUnprocessedParsedTransactionsUseCase)
    factoryOf(::InsertParsedTransactionUseCase)
    factoryOf(::MarkParsedTransactionProcessedUseCase)
    factoryOf(::DeleteParsedTransactionUseCase)

    // Data Backup/Restore Use Cases
    factoryOf(::ExportDataUseCase)
    factoryOf(::ImportDataUseCase)

    // Category Use Cases
    factoryOf(::GetCategoriesUseCase)
    factoryOf(::AddCategoryUseCase)
    factoryOf(::DeleteCategoryUseCase)
}