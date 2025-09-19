package com.woojin.paymanagement.di

import org.koin.dsl.module

val preferencesModule = module {
    // PreferencesManager는 플랫폼별로 제공됨
    // 플랫폼에서 직접 제공하므로 여기서는 별도 정의 불필요
}