package com.woojin.paymanagement.presentation.paydaysetup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woojin.paymanagement.domain.model.PaydaySetup
import com.woojin.paymanagement.domain.usecase.GetPaydaySetupUseCase
import com.woojin.paymanagement.domain.usecase.SavePaydaySetupUseCase
import com.woojin.paymanagement.domain.usecase.ValidatePaydaySetupUseCase
import com.woojin.paymanagement.domain.usecase.FetchHolidaysUseCase
import com.woojin.paymanagement.utils.PaydayAdjustment
import com.woojin.paymanagement.utils.PayPeriodCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class PaydaySetupViewModel(
    private val getPaydaySetupUseCase: GetPaydaySetupUseCase,
    private val savePaydaySetupUseCase: SavePaydaySetupUseCase,
    private val validatePaydaySetupUseCase: ValidatePaydaySetupUseCase,
    private val fetchHolidaysUseCase: FetchHolidaysUseCase,
    private val payPeriodCalculator: PayPeriodCalculator
) : ViewModel() {

    companion object {
        private val HOLIDAY_API_KEY = com.woojin.paymanagement.BuildKonfig.HOLIDAY_API_KEY
    }

    var uiState by mutableStateOf(PaydaySetupUiState())
        private set

    private var previewJob: Job? = null

    init {
        loadCurrentSetup()
        refreshPreview()
        preloadHolidays()
    }

    fun selectPayday(payday: Int) {
        if (payday in 1..31) {
            uiState = uiState.copy(selectedPayday = payday, error = null)
            refreshPreview()
        }
    }

    fun selectAdjustment(adjustment: PaydayAdjustment) {
        uiState = uiState.copy(selectedAdjustment = adjustment, error = null)
        refreshPreview()
    }

    /**
     * 공휴일 데이터를 미리 받아 미리보기에 공휴일 조정까지 반영 (없는 연도만 받음, 실패해도 무시)
     */
    private fun preloadHolidays() {
        viewModelScope.launch {
            try {
                fetchHolidaysUseCase(HOLIDAY_API_KEY).onSuccess { refreshPreview() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 미리보기용이라 실패해도 그대로 진행 (설정 완료 시 한 번 더 시도함)
            }
        }
    }

    /**
     * 고른 월급날·조정 방식 기준으로 이번 급여 기간과 다음 실제 월급날 계산 (화면 상단 미리보기)
     */
    private fun refreshPreview() {
        val payday = uiState.selectedPayday
        val adjustment = uiState.selectedAdjustment
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            try {
                val period = payPeriodCalculator.getCurrentPayPeriod(payday, adjustment)
                val nextPayday = period.endDate.plus(1, DateTimeUnit.DAY)

                // 다음 월급날의 원래(조정 전) 날짜 찾기. 조정으로 이웃 달로 넘어갈 수 있어 앞뒤 달까지 확인
                var nominal = nextPayday
                for (offset in -1..1) {
                    val monthDate = nextPayday.plus(offset, DateTimeUnit.MONTH)
                    val actual = payPeriodCalculator.calculateActualPayday(monthDate.year, monthDate.month, payday, adjustment)
                    if (actual == nextPayday) {
                        val firstDay = LocalDate(monthDate.year, monthDate.month, 1)
                        val lastDay = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
                        nominal = LocalDate(monthDate.year, monthDate.month, minOf(payday, lastDay))
                        break
                    }
                }

                uiState = uiState.copy(
                    currentPeriod = period,
                    nextPayday = nextPayday,
                    nextPaydayNominal = nominal
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 미리보기 계산 실패 시 미리보기만 숨김
            }
        }
    }

    fun completeSetup() {
        val validationResult = validatePaydaySetupUseCase(
            uiState.selectedPayday,
            uiState.selectedAdjustment
        )

        if (!validationResult.isValid) {
            uiState = uiState.copy(error = validationResult.errorMessage)
            return
        }

        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, error = null)

                val paydaySetup = PaydaySetup(
                    payday = uiState.selectedPayday,
                    adjustment = uiState.selectedAdjustment
                )

                savePaydaySetupUseCase(paydaySetup)

                // 공휴일 데이터 가져오기 (백그라운드에서 실행, 실패해도 설정 완료)
                fetchHolidaysUseCase(HOLIDAY_API_KEY).onFailure { exception ->
                    println("공휴일 데이터 가져오기 실패: ${exception.message}")
                }

                uiState = uiState.copy(
                    isLoading = false,
                    isSetupComplete = true
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message ?: "설정 저장 중 오류가 발생했습니다."
                )
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    private fun loadCurrentSetup() {
        try {
            val currentSetup = getPaydaySetupUseCase()
            uiState = uiState.copy(
                selectedPayday = currentSetup.payday,
                selectedAdjustment = currentSetup.adjustment
            )
        } catch (e: Exception) {
            // Use default values if loading fails
        }
    }
}