package com.woojin.paymanagement.utils

import androidx.compose.runtime.Composable

/**
 * 플랫폼별 뒤로가기 핸들러
 * Android: 시스템 뒤로 버튼 처리
 * iOS: 빈 구현 (iOS는 자체 네비게이션 제스처 사용)
 */
@Composable
expect fun PlatformBackHandler(onBack: () -> Unit)
