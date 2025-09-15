package com.woojin.paymanagement.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = PayManagementDatabase.Schema,
            context = context,
            name = "PayManagementDatabase.db"
        )
    }
}