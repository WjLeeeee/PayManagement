package com.woojin.paymanagement.utils

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler as AndroidBackHandler

/**
 * Android actual 구현
 * Android의 시스템 뒤로가기 버튼 처리
 */
@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    AndroidBackHandler(enabled = enabled, onBack = onBack)
}