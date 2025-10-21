package com.woojin.paymanagement.utils

import androidx.compose.runtime.Composable

/**
 * iOS actual 구현
 * iOS는 시스템 레벨 뒤로가기 버튼이 없으므로 아무 동작도 하지 않음
 */
@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // iOS에서는 시스템 뒤로가기 버튼이 없으므로 아무 동작도 하지 않음
}