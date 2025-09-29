package com.woojin.paymanagement.presentation.tutorial

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.woojin.paymanagement.utils.PreferencesManager

class CalendarTutorialViewModel(
    private val preferencesManager: PreferencesManager
) {
    var uiState by mutableStateOf(CalendarTutorialUiState())
        private set

    private val tutorialSteps = listOf(
        TutorialStep(
            id = "pay_period_summary",
            targetBounds = null, // Will be set when UI element is measured
            title = "💰 급여기간 요약",
            description = "이 영역을 터치하면 해당 급여기간의 상세한 통계 화면으로 이동할 수 있어요. 수입과 지출을 한눈에 확인해보세요!",
            tooltipPosition = TooltipPosition.BOTTOM
        ),
        TutorialStep(
            id = "transaction_card",
            targetBounds = null,
            title = "📅 일별 거래내역",
            description = "날짜를 선택한 후 거래내역 카드를 터치하면 해당 날짜의 상세 정보를 볼 수 있어요. 거래 내역을 수정하거나 추가할 수 있답니다!",
            tooltipPosition = TooltipPosition.TOP
        ),
        TutorialStep(
            id = "floating_action_button",
            targetBounds = null,
            title = "➕ 거래 추가하기",
            description = "이 버튼을 터치하면 새로운 수입이나 지출을 빠르게 추가할 수 있어요. 가계부 작성이 더욱 쉬워집니다!",
            tooltipPosition = TooltipPosition.TOP
        )
    )

    init {
        checkTutorialState()
    }

    private fun checkTutorialState() {
        if (!preferencesManager.isCalendarTutorialCompleted()) {
            uiState = uiState.copy(
                shouldShowTutorial = true,
                currentStepIndex = 0,
                steps = tutorialSteps,
                currentStep = tutorialSteps.getOrNull(0)
            )
        }
    }

    fun updateTargetBounds(stepId: String, bounds: Rect) {
        val currentSteps = uiState.steps.ifEmpty { tutorialSteps }
        val updatedSteps = currentSteps.map { step ->
            if (step.id == stepId) {
                step.copy(targetBounds = bounds)
            } else {
                step
            }
        }

        uiState = uiState.copy(
            steps = updatedSteps,
            currentStep = updatedSteps.getOrNull(uiState.currentStepIndex)
        )
    }

    fun nextStep() {
        val currentSteps = uiState.steps.ifEmpty { tutorialSteps }
        if (uiState.currentStepIndex < currentSteps.size - 1) {
            val nextIndex = uiState.currentStepIndex + 1
            uiState = uiState.copy(
                currentStepIndex = nextIndex,
                currentStep = currentSteps.getOrNull(nextIndex)
            )
        } else {
            completeTutorial()
        }
    }

    fun skipTutorial() {
        completeTutorial()
    }

    fun completeTutorial() {
        preferencesManager.setCalendarTutorialCompleted()
        uiState = uiState.copy(
            shouldShowTutorial = false,
            currentStep = null
        )
    }

    fun startTutorial() {
        uiState = uiState.copy(
            shouldShowTutorial = true,
            currentStepIndex = 0,
            steps = tutorialSteps,
            currentStep = tutorialSteps.getOrNull(0)
        )
    }

    fun getTargetBounds(stepId: String): Rect? {
        val currentSteps = uiState.steps.ifEmpty { tutorialSteps }
        return currentSteps.find { it.id == stepId }?.targetBounds
    }
}

data class CalendarTutorialUiState(
    val shouldShowTutorial: Boolean = false,
    val currentStepIndex: Int = 0,
    val steps: List<TutorialStep> = emptyList(),
    val currentStep: TutorialStep? = null
)