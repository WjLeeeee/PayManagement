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
import kotlinx.coroutines.launch

class PaydaySetupViewModel(
    private val getPaydaySetupUseCase: GetPaydaySetupUseCase,
    private val savePaydaySetupUseCase: SavePaydaySetupUseCase,
    private val validatePaydaySetupUseCase: ValidatePaydaySetupUseCase,
    private val fetchHolidaysUseCase: FetchHolidaysUseCase
) : ViewModel() {

    companion object {
        private val HOLIDAY_API_KEY = com.woojin.paymanagement.BuildKonfig.HOLIDAY_API_KEY
    }
    var uiState by mutableStateOf(PaydaySetupUiState())
        private set

    init {
        loadCurrentSetup()
    }

    fun selectPayday(payday: Int) {
        if (payday in 1..31) {
            uiState = uiState.copy(selectedPayday = payday, error = null)
        }
    }

    fun selectAdjustment(adjustment: PaydayAdjustment) {
        uiState = uiState.copy(selectedAdjustment = adjustment, error = null)
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
                    // 로그만 남기고 설정은 완료
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