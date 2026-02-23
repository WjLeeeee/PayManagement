package com.woojin.paymanagement.presentation.adremoval

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woojin.paymanagement.domain.repository.BillingRepository
import com.woojin.paymanagement.utils.BillingResult
import com.woojin.paymanagement.utils.AdRemovalProductId
import com.woojin.paymanagement.utils.PreferencesManager
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * 광고 제거 화면의 ViewModel
 * Clean Architecture의 Presentation Layer에 위치
 */
class AdRemovalViewModel(
    private val billingRepository: BillingRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    var uiState by mutableStateOf(AdRemovalUiState())
        private set

    init {
        // 결제 클라이언트 초기화
        billingRepository.initialize {
            updateUiState { copy(isBillingReady = true) }
        }
    }

    /**
     * 광고 제거 기간 선택
     */
    fun selectAdRemovalPeriod(period: AdRemovalPeriod?) {
        updateUiState { copy(selectedPeriod = period) }
    }

    /**
     * 결제 시작
     */
    fun purchaseAdRemoval() {
        val selectedPeriod = uiState.selectedPeriod ?: return

        updateUiState { copy(isPurchasing = true, purchaseError = null) }

        viewModelScope.launch {
            try {
                val productId = when (selectedPeriod) {
                    AdRemovalPeriod.ONE_DAY -> AdRemovalProductId.ONE_DAY
                    AdRemovalPeriod.THREE_DAYS -> AdRemovalProductId.THREE_DAYS
                    AdRemovalPeriod.SEVEN_DAYS -> AdRemovalProductId.SEVEN_DAYS
                    AdRemovalPeriod.THIRTY_DAYS -> AdRemovalProductId.THIRTY_DAYS
                }

                val result = billingRepository.launchPurchaseFlow(productId)

                when (result) {
                    is BillingResult.Success -> {
                        // 구매 성공 - 만료 시간 계산 및 저장
                        val currentTime = Clock.System.now().toEpochMilliseconds()
                        val daysInMillis = selectedPeriod.days * 24 * 60 * 60 * 1000L
                        val expiryTime = currentTime + daysInMillis

                        preferencesManager.setAdRemovalExpiryTime(expiryTime)

                        updateUiState {
                            copy(
                                isPurchasing = false,
                                showSuccessDialog = true,
                                selectedPeriod = null
                            )
                        }
                    }
                    is BillingResult.Error -> {
                        updateUiState {
                            copy(
                                isPurchasing = false,
                                purchaseError = result.message
                            )
                        }
                    }
                    is BillingResult.Canceled -> {
                        updateUiState {
                            copy(
                                isPurchasing = false,
                                purchaseError = null
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                updateUiState {
                    copy(
                        isPurchasing = false,
                        purchaseError = "결제 중 오류가 발생했습니다: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 성공 다이얼로그 닫기
     */
    fun dismissSuccessDialog() {
        updateUiState { copy(showSuccessDialog = false) }
    }

    /**
     * 에러 메시지 닫기
     */
    fun dismissError() {
        updateUiState { copy(purchaseError = null) }
    }

    /**
     * UI 상태 업데이트 헬퍼 함수
     */
    private fun updateUiState(update: AdRemovalUiState.() -> AdRemovalUiState) {
        uiState = uiState.update()
    }

    override fun onCleared() {
        super.onCleared()
        billingRepository.disconnect()
    }
}

/**
 * 광고 제거 화면 UI 상태
 */
data class AdRemovalUiState(
    val selectedPeriod: AdRemovalPeriod? = null,
    val isBillingReady: Boolean = false,
    val isPurchasing: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val purchaseError: String? = null
)

/**
 * 광고 제거 기간 옵션
 */
enum class AdRemovalPeriod(val krw: String, val days: Int, val description: String) {
    ONE_DAY("100", 1, "1일"),
    THREE_DAYS("250", 3, "3일"),
    SEVEN_DAYS("500", 7, "7일"),
    THIRTY_DAYS("2,000", 30, "30일")
}
