package com.woojin.paymanagement.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * iOS 플랫폼에서 Lifecycle 이벤트를 감지하는 구현체
 *
 * 현재는 비활성화 (NSNotificationCenter 사용 시 selector 문제 발생)
 */
actual class LifecycleObserverHelper {
    @Composable
    actual fun ObserveLifecycle(onResume: () -> Unit) {
        DisposableEffect(Unit) {
            // iOS에서는 라이프사이클 감지 비활성화
            // NSNotificationCenter의 selector 등록이 Kotlin/Native에서 제대로 작동하지 않음
            onDispose {
                // 정리 작업 없음
            }
        }
    }
}
