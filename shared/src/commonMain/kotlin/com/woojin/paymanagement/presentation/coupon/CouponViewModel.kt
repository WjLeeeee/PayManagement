package com.woojin.paymanagement.presentation.coupon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.woojin.paymanagement.utils.PreferencesManager
import kotlinx.datetime.Clock

/**
 * 쿠폰 입력 화면의 ViewModel
 */
class CouponViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    var uiState by mutableStateOf(CouponUiState())
        private set

    companion object {
        private const val VALID_COUPON_CODE = "Quinones0115"
        private const val COUPON_AD_REMOVAL_DAYS = 3
    }

    /**
     * 쿠폰 코드 입력
     */
    fun onCouponCodeChange(code: String) {
        updateUiState { copy(couponCode = code, errorMessage = null) }
    }

    /**
     * 쿠폰 적용
     */
    fun applyCoupon() {
        val code = uiState.couponCode.trim()

        if (code.isEmpty()) {
            updateUiState { copy(errorMessage = "쿠폰 코드를 입력해주세요.") }
            return
        }

        updateUiState { copy(isApplying = true, errorMessage = null) }

        // 쿠폰 코드 확인
        if (code == VALID_COUPON_CODE) {
            // 쿠폰이 이미 사용되었는지 확인
            if (preferencesManager.isCouponUsed(code)) {
                updateUiState {
                    copy(
                        isApplying = false,
                        errorMessage = "이미 사용된 쿠폰입니다."
                    )
                }
                return
            }

            // 쿠폰 성공 - 3일 광고제거 적용
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val daysInMillis = COUPON_AD_REMOVAL_DAYS * 24 * 60 * 60 * 1000L
            val expiryTime = currentTime + daysInMillis

            preferencesManager.setAdRemovalExpiryTime(expiryTime)
            preferencesManager.markCouponAsUsed(code)

            updateUiState {
                copy(
                    isApplying = false,
                    showSuccessDialog = true,
                    couponCode = ""
                )
            }
        } else {
            // 쿠폰 불일치
            updateUiState {
                copy(
                    isApplying = false,
                    errorMessage = "일치하지 않는 쿠폰번호입니다."
                )
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
        updateUiState { copy(errorMessage = null) }
    }

    /**
     * UI 상태 업데이트 헬퍼 함수
     */
    private fun updateUiState(update: CouponUiState.() -> CouponUiState) {
        uiState = uiState.update()
    }
}

/**
 * 쿠폰 입력 화면 UI 상태
 */
data class CouponUiState(
    val couponCode: String = "",
    val isApplying: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val errorMessage: String? = null
)
