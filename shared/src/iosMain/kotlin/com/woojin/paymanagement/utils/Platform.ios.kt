package com.woojin.paymanagement.utils

actual object Platform {
    actual fun isAndroid(): Boolean = false

    actual fun isIOS(): Boolean = true
}
