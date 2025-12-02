package com.woojin.paymanagement.android.di

import com.woojin.paymanagement.android.analytics.AnalyticsTracker
import com.woojin.paymanagement.android.analytics.FirebaseAnalyticsTracker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val analyticsModule = module {
    single<AnalyticsTracker> {
        FirebaseAnalyticsTracker(androidContext())
    }
}
