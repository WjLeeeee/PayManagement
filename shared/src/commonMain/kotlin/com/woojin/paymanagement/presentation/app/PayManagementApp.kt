package com.woojin.paymanagement.presentation.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.database.DatabaseDriverFactory
import com.woojin.paymanagement.di.databaseModule
import com.woojin.paymanagement.di.domainModule
import com.woojin.paymanagement.di.presentationModule
import com.woojin.paymanagement.domain.model.Screen
import com.woojin.paymanagement.presentation.addtransaction.AddTransactionScreen
import com.woojin.paymanagement.presentation.calendar.CalendarScreen
import com.woojin.paymanagement.presentation.calendar.CalendarViewModel
import com.woojin.paymanagement.presentation.datedetail.DateDetailScreen
import com.woojin.paymanagement.presentation.paydaysetup.PaydaySetupScreen
import com.woojin.paymanagement.presentation.statistics.StatisticsScreen
import com.woojin.paymanagement.utils.PreferencesManager
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module

// Koin 인스턴스를 저장할 변수
var koinInstance: Koin? = null

// Koin 의존성 주입을 위한 헬퍼 함수
inline fun <reified T> koinInject(): T = requireNotNull(koinInstance).get()

/**
 * 메인 앱 진입점
 * Clean Architecture: 의존성 주입 초기화만 담당
 */
@Composable
fun App(databaseDriverFactory: DatabaseDriverFactory, preferencesManager: PreferencesManager) {
    var isKoinInitialized by remember { mutableStateOf(false) }

    // Koin 초기화
    LaunchedEffect(Unit) {
        initializeKoin(databaseDriverFactory, preferencesManager)
        isKoinInitialized = true
    }

    MaterialTheme {
        if (isKoinInitialized) {
            PayManagementApp()
        } else {
            // 로딩 화면 또는 빈 화면
        }
    }
}

/**
 * Koin 초기화 함수
 */
private fun initializeKoin(
    databaseDriverFactory: DatabaseDriverFactory,
    preferencesManager: PreferencesManager
) {
    try {
        val koin = startKoin {
            modules(
                // 플랫폼별 의존성들을 동적으로 제공하는 모듈
                module {
                    single<DatabaseDriverFactory> { databaseDriverFactory }
                    single<PreferencesManager> { preferencesManager }
                },
                // 공통 의존성들
                databaseModule,
                domainModule,
                presentationModule
            )
        }.koin

        // 전역 변수에 Koin 인스턴스 저장
        koinInstance = koin
    } catch (e: Exception) {
        // 이미 초기화된 경우 무시
        println("Koin already initialized: ${e.message}")
    }
}

/**
 * 메인 앱 컴포저블
 * Clean Architecture: 순수 UI, 모든 로직은 ViewModel에 위임
 */
@Composable
fun PayManagementApp() {
    // AppViewModel 주입
    val appViewModel: AppViewModel = koinInject()
    val uiState = appViewModel.uiState

    // 화면별 렌더링
    when (uiState.navigationState.currentScreen) {
        Screen.PaydaySetup -> {
            PaydaySetupScreen(
                viewModel = koinInject(),
                onSetupComplete = { payday, adjustment ->
                    appViewModel.onPaydaySetupComplete(payday, adjustment)
                }
            )
        }

        Screen.Calendar -> {
            val calendarViewModel: CalendarViewModel = koinInject()

            // CalendarViewModel 초기화
            LaunchedEffect(uiState.appData.transactions, uiState.navigationState.selectedPayPeriod) {
                calendarViewModel.initializeCalendar(
                    transactions = uiState.appData.transactions,
                    initialPayPeriod = uiState.navigationState.selectedPayPeriod,
                    selectedDate = uiState.navigationState.selectedDate
                )
            }

            CalendarScreen(
                viewModel = calendarViewModel,
                tutorialViewModel = koinInject(),
                onDateDetailClick = appViewModel::navigateToDateDetail,
                onStatisticsClick = appViewModel::navigateToStatistics,
                onAddTransactionClick = { appViewModel.navigateToAddTransaction() },
                onPayPeriodChanged = appViewModel::updateCalendarPayPeriod
            )
        }

        Screen.Statistics -> {
            StatisticsScreen(
                transactions = uiState.appData.transactions,
                availableBalanceCards = uiState.appData.availableBalanceCards,
                availableGiftCards = uiState.appData.availableGiftCards,
                initialPayPeriod = uiState.navigationState.selectedPayPeriod,
                onBack = appViewModel::navigateToCalendar,
                viewModel = koinInject()
            )
        }

        Screen.AddTransaction -> {
            AddTransactionScreen(
                transactions = uiState.appData.transactions,
                selectedDate = uiState.navigationState.selectedDate
                    ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
                editTransaction = uiState.navigationState.editTransaction,
                viewModel = koinInject(),
                onSave = appViewModel::saveTransactions,
                onCancel = appViewModel::navigateBack
            )
        }

        Screen.DateDetail -> {
            DateDetailScreen(
                selectedDate = uiState.navigationState.selectedDate
                    ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
                transactions = uiState.appData.transactions,
                viewModel = koinInject(),
                onBack = appViewModel::navigateToCalendar,
                onEditTransaction = { transaction ->
                    appViewModel.navigateToAddTransaction(transaction)
                },
                onDeleteTransaction = appViewModel::deleteTransaction,
                onAddTransaction = { appViewModel.navigateToAddTransaction() }
            )
        }

        Screen.EditTransaction -> {
            // EditTransaction은 AddTransaction과 동일하게 처리
            AddTransactionScreen(
                transactions = uiState.appData.transactions,
                selectedDate = uiState.navigationState.selectedDate
                    ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
                editTransaction = uiState.navigationState.editTransaction,
                viewModel = koinInject(),
                onSave = appViewModel::saveTransactions,
                onCancel = appViewModel::navigateBack
            )
        }
    }

    // 에러 메시지 표시 (필요시)
    uiState.error?.let { error ->
        // 에러 스낵바나 다이얼로그 표시
        LaunchedEffect(error) {
            println("App Error: $error")
            appViewModel.clearError()
        }
    }
}