package com.woojin.paymanagement.utils

import androidx.compose.runtime.Composable

/**
 * 플랫폼별 Lifecycle 이벤트를 감지하기 위한 인터페이스
 */
expect class LifecycleObserverHelper() {
    /**
     * Composable 함수 내에서 라이프사이클 이벤트를 관찰합니다.
     * @param onResume 화면이 다시 포커스를 받았을 때 호출되는 콜백
     */
    @Composable
    fun ObserveLifecycle(onResume: () -> Unit)
}
