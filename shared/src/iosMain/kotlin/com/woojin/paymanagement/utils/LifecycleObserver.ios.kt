package com.woojin.paymanagement.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_async

/**
 * iOS 플랫폼에서 Lifecycle 이벤트를 감지하는 구현체
 * UIApplicationDidBecomeActiveNotification을 통해 앱이 활성화될 때를 감지합니다.
 */
actual class LifecycleObserverHelper {
    @OptIn(ExperimentalForeignApi::class)
    @Composable
    actual fun ObserveLifecycle(onResume: () -> Unit) {
        DisposableEffect(Unit) {
            val notificationCenter = NSNotificationCenter.defaultCenter
            val observer = object : NSObject() {
                @Suppress("unused")
                fun applicationDidBecomeActive() {
                    // 메인 스레드에서 콜백 실행
                    dispatch_async(dispatch_get_main_queue()) {
                        onResume()
                    }
                }
            }

            notificationCenter.addObserver(
                observer = observer,
                selector = platform.objc.sel_registerName("applicationDidBecomeActive"),
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null
            )

            onDispose {
                notificationCenter.removeObserver(observer)
            }
        }
    }
}
