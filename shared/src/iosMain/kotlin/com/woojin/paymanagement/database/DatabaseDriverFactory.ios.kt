package com.woojin.paymanagement.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return try {
            NativeSqliteDriver(
                schema = PayManagementDatabase.Schema,
                name = "PayManagementDatabase.db",
                onConfiguration = { config ->
                    config.copy(
                        version = 6
                    )
                }
            )
        } catch (e: Exception) {
            // 오류 발생 시 재시도
            NativeSqliteDriver(
                schema = PayManagementDatabase.Schema,
                name = "PayManagementDatabase.db",
                onConfiguration = { config ->
                    config.copy(
                        version = 6
                    )
                }
            )
        }
    }
}