package com.woojin.paymanagement

import androidx.compose.ui.window.ComposeUIViewController
import com.woojin.paymanagement.database.DatabaseDriverFactory
import com.woojin.paymanagement.utils.PreferencesManager

fun MainViewController() = ComposeUIViewController {
    App(
        databaseDriverFactory = DatabaseDriverFactory(),
        preferencesManager = PreferencesManager()
    )
}