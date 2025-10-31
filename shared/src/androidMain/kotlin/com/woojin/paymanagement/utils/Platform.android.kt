package com.woojin.paymanagement.utils

actual object Platform {
    actual fun isAndroid(): Boolean = true

    actual fun isIOS(): Boolean = false
}
