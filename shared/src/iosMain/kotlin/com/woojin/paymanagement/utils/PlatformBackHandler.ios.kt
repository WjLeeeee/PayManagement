package com.woojin.paymanagement.utils

import androidx.compose.runtime.Composable

/**
 * iOS에서는 빈 구현
 * iOS는 자체 스와이프 제스처로 뒤로가기를 처리
 */
@Composable
actual fun PlatformBackHandler(onBack: () -> Unit) {
    // iOS는 시스템 뒤로 버튼이 없으므로 아무것도 하지 않음
}
