package com.woojin.paymanagement.utils

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android에서는 시스템 뒤로 버튼을 처리
 */
@Composable
actual fun PlatformBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
