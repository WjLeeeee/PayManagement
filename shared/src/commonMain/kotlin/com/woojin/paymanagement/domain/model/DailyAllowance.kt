package com.woojin.paymanagement.domain.model

/**
 * 월급날까지 하루에 쓸 수 있는 돈
 * dailyLimit은 오늘 지출을 빼고 계산해서 하루 동안 고정, 오늘 지출만큼 remainingToday가 줄어듦
 */
data class DailyAllowance(
    val dailyLimit: Double,  // 오늘 쓸 수 있는 돈 (음수면 이미 예산 초과)
    val todaySpent: Double,  // 오늘 지출
    val daysLeft: Int        // 오늘 포함 다음 급여일 전날까지 남은 일수
) {
    val remainingToday: Double get() = dailyLimit - todaySpent
}
