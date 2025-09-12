package com.woojin.paymanagement

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform