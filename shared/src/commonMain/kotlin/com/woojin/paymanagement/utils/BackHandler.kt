package com.woojin.paymanagement.utils

import androidx.compose.runtime.Composable

/**
 * 플랫폼별 뒤로가기 처리를 위한 Composable
 * Android: 시스템 뒤로가기 버튼 처리
 * iOS: 아무 동작 안함 (iOS는 시스템 뒤로가기 버튼이 없음)
 */
@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)