package com.woojin.paymanagement.presentation.tipdonation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woojin.paymanagement.domain.repository.BillingRepository
import com.woojin.paymanagement.domain.usecase.PurchaseTipUseCase
import com.woojin.paymanagement.utils.BillingResult
import com.woojin.paymanagement.utils.TipProductId
import kotlinx.coroutines.launch

/**
 * 팁주기 화면의 ViewModel
 * Clean Architecture의 Presentation Layer에 위치
 *
 * 단일 책임 원칙(SRP): 팁주기 UI 상태 관리만 담당
 */
class TipDonationViewModel(
    private val purchaseTipUseCase: PurchaseTipUseCase,
    private val billingRepository: BillingRepository
) : ViewModel() {

    var uiState by mutableStateOf(TipDonationUiState())
        private set

    init {
        // 결제 클라이언트 초기화
        billingRepository.initialize {
            updateUiState { copy(isBillingReady = true) }
        }
    }

    /**
     * 팁 금액 선택
     */
    fun selectTipAmount(tipAmount: TipAmount?) {
        updateUiState { copy(selectedTipAmount = tipAmount) }
    }

    /**
     * 결제 시작
     */
    fun purchaseTip() {
        val selectedAmount = uiState.selectedTipAmount ?: return

        updateUiState { copy(isPurchasing = true, purchaseError = null) }

        viewModelScope.launch {
            try {
                val productId = when (selectedAmount) {
                    TipAmount.COFFEE -> TipProductId.COFFEE
                    TipAmount.LUNCH -> TipProductId.LUNCH
                    TipAmount.DINNER -> TipProductId.DINNER
                }

                val result = purchaseTipUseCase(productId)

                when (result) {
                    is BillingResult.Success -> {
                        updateUiState {
                            copy(
                                isPurchasing = false,
                                showThankYouDialog = true,
                                selectedTipAmount = null
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
     * 감사 다이얼로그 닫기
     */
    fun dismissThankYouDialog() {
        updateUiState { copy(showThankYouDialog = false) }
    }

    /**
     * 에러 메시지 닫기
     */
    fun dismissError() {
        updateUiState { copy(purchaseError = null) }
    }

    /**
     * UI 상태 업데이트 헬퍼 함수 (중복 코드 최소화)
     */
    private fun updateUiState(update: TipDonationUiState.() -> TipDonationUiState) {
        uiState = uiState.update()
    }

    override fun onCleared() {
        super.onCleared()
        billingRepository.disconnect()
    }
}

/**
 * 팁주기 화면 UI 상태
 */
data class TipDonationUiState(
    val selectedTipAmount: TipAmount? = null,
    val isBillingReady: Boolean = false,
    val isPurchasing: Boolean = false,
    val showThankYouDialog: Boolean = false,
    val purchaseError: String? = null
)

/**
 * 팁 금액 옵션
 */
enum class TipAmount(val krw: String) {
    COFFEE("1,000"),
    LUNCH("5,000"),
    DINNER("10,000")
}
