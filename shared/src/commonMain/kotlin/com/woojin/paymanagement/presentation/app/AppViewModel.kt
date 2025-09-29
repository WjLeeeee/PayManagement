package com.woojin.paymanagement.presentation.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.model.AppData
import com.woojin.paymanagement.domain.model.AppState
import com.woojin.paymanagement.domain.model.NavigationState
import com.woojin.paymanagement.domain.model.Screen
import com.woojin.paymanagement.domain.repository.TransactionRepository
import com.woojin.paymanagement.domain.usecase.ProcessTransactionWithCardsUseCase
import com.woojin.paymanagement.domain.usecase.navigation.NavigateToScreenUseCase
import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * 앱 전체 상태를 관리하는 ViewModel
 * SRP: 앱 상태 관리와 비즈니스 로직 호출만 담당
 * DIP: UseCase들과 Repository 인터페이스에 의존
 */
class AppViewModel(
    private val preferencesManager: PreferencesManager,
    private val transactionRepository: TransactionRepository,
    private val navigateToScreenUseCase: NavigateToScreenUseCase,
    private val processTransactionWithCardsUseCase: ProcessTransactionWithCardsUseCase,
    private val coroutineScope: CoroutineScope
) {

    var uiState by mutableStateOf(AppState())
        private set

    init {
        initializeApp()
    }

    /**
     * 앱 초기화
     */
    private fun initializeApp() {
        // 초기 화면 결정
        val initialScreen = when {
            !preferencesManager.isPaydaySet() -> Screen.PaydaySetup
            else -> Screen.Calendar
        }

        // 초기 선택 날짜 설정
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        uiState = uiState.copy(
            navigationState = NavigationState(
                currentScreen = initialScreen,
                selectedDate = today
            ),
            isKoinInitialized = true
        )

        // 데이터 로딩 시작
        loadAppData()
    }

    /**
     * 앱 데이터 로딩 (거래, 잔액권, 상품권)
     */
    private fun loadAppData() {
        // 거래 내역 로딩
        coroutineScope.launch {
            transactionRepository.getAllTransactions()
                .catch { error ->
                    updateError("거래 내역 로딩 실패: ${error.message}")
                }
                .collect { transactions ->
                    updateAppData { it.copy(transactions = transactions) }
                }
        }

        // 잔액권 로딩
        coroutineScope.launch {
            transactionRepository.getActiveBalanceCards()
                .catch { error ->
                    updateError("잔액권 로딩 실패: ${error.message}")
                }
                .collect { cards ->
                    updateAppData { it.copy(availableBalanceCards = cards) }
                }
        }

        // 상품권 로딩
        coroutineScope.launch {
            transactionRepository.getActiveGiftCards()
                .catch { error ->
                    updateError("상품권 로딩 실패: ${error.message}")
                }
                .collect { cards ->
                    updateAppData { it.copy(availableGiftCards = cards) }
                }
        }
    }

    // === Navigation Methods ===

    /**
     * 급여일 설정 완료
     */
    fun onPaydaySetupComplete(payday: Int, adjustment: com.woojin.paymanagement.utils.PaydayAdjustment) {
        preferencesManager.setPayday(payday)
        preferencesManager.setPaydayAdjustment(adjustment)

        val newNavigationState = navigateToScreenUseCase.navigateToCalendarFromSetup(
            uiState.navigationState
        )
        updateNavigationState(newNavigationState)
    }

    /**
     * 날짜 상세 화면으로 이동
     */
    fun navigateToDateDetail(date: LocalDate) {
        val newNavigationState = navigateToScreenUseCase.navigateToDateDetail(
            uiState.navigationState,
            date
        )
        updateNavigationState(newNavigationState)
    }

    /**
     * 통계 화면으로 이동
     */
    fun navigateToStatistics(payPeriod: PayPeriod) {
        val newNavigationState = navigateToScreenUseCase.navigateToStatistics(
            uiState.navigationState,
            payPeriod
        )
        updateNavigationState(newNavigationState)
    }

    /**
     * 거래 추가 화면으로 이동
     */
    fun navigateToAddTransaction(editTransaction: Transaction? = null) {
        val newNavigationState = navigateToScreenUseCase.navigateToAddTransaction(
            uiState.navigationState,
            editTransaction,
            uiState.navigationState.currentScreen
        )
        updateNavigationState(newNavigationState)
    }

    /**
     * 이전 화면으로 돌아가기
     */
    fun navigateBack() {
        val newNavigationState = navigateToScreenUseCase.navigateBack(uiState.navigationState)
        updateNavigationState(newNavigationState)
    }

    /**
     * 캘린더 화면으로 돌아가기
     */
    fun navigateToCalendar() {
        val newNavigationState = navigateToScreenUseCase.navigateToCalendar(uiState.navigationState)
        updateNavigationState(newNavigationState)
    }

    /**
     * 급여 기간 변경
     */
    fun updateCalendarPayPeriod(payPeriod: PayPeriod) {
        val newNavigationState = navigateToScreenUseCase.updateCalendarPayPeriod(
            uiState.navigationState,
            payPeriod
        )
        updateNavigationState(newNavigationState)
    }

    // === Transaction Methods ===

    /**
     * 거래 저장 (카드 관리 포함)
     */
    fun saveTransactions(transactions: List<Transaction>) {
        coroutineScope.launch {
            setLoading(true)

            try {
                if (uiState.navigationState.editTransaction != null) {
                    // 편집 모드: 거래 업데이트는 이미 UseCase에서 처리됨
                    // 현재는 별도 처리 없음
                } else {
                    // 추가 모드: 새 거래들 추가 및 카드 관리
                    processTransactionWithCardsUseCase.processMultipleTransactions(transactions)
                }

                // 저장 완료 후 이전 화면으로 돌아가기
                navigateBack()
            } catch (error: Exception) {
                updateError("거래 저장 실패: ${error.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * 거래 삭제
     */
    fun deleteTransaction(transaction: Transaction) {
        coroutineScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction.id)
            } catch (e: Exception) {
                updateError("거래 삭제 실패: ${e.message}")
            }
        }
    }

    // === Private Helper Methods ===

    private fun updateNavigationState(navigationState: NavigationState) {
        uiState = uiState.copy(navigationState = navigationState)
    }

    private fun updateAppData(update: (AppData) -> AppData) {
        uiState = uiState.copy(appData = update(uiState.appData))
    }

    private fun setLoading(isLoading: Boolean) {
        uiState = uiState.copy(isLoading = isLoading)
    }

    private fun updateError(error: String?) {
        uiState = uiState.copy(error = error)
    }

    /**
     * 에러 메시지 클리어
     */
    fun clearError() {
        updateError(null)
    }
}