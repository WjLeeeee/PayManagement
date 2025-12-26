package com.woojin.paymanagement.utils

import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS 이메일 전송 헬퍼
 */
actual class EmailHelper {
    @OptIn(ExperimentalForeignApi::class)
    actual fun sendSupportEmail(
        email: String,
        subject: String,
        appVersion: String,
        osVersion: String,
        deviceModel: String
    ) {
        val body = """


iOS 앱버전 : $appVersion
iOS 버전 : $osVersion($deviceModel)
오류 및 건의사항 편하게 문의주세요 :)

        """.trimIndent()

        // 간단한 URL 인코딩 (공백을 %20으로 변경)
        val encodedSubject = subject.replace(" ", "%20")
        val encodedBody = body.replace("\n", "%0A").replace(" ", "%20")

        // mailto URL 생성
        val urlString = "mailto:$email?subject=$encodedSubject&body=$encodedBody"
        val url = NSURL.URLWithString(urlString)

        // 메인 스레드에서 메일 앱 열기
        dispatch_async(dispatch_get_main_queue()) {
            url?.let {
                if (UIApplication.sharedApplication.canOpenURL(it)) {
                    UIApplication.sharedApplication.openURL(it)
                }
            }
        }
    }
}
