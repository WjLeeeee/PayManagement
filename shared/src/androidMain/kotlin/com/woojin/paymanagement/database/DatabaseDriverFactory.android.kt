package com.woojin.paymanagement.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(
            schema = PayManagementDatabase.Schema,
            context = context,
            name = "PayManagementDatabase.db"
        )

        // ParsedTransactionEntity 테이블이 없으면 생성 (CREATE TABLE IF NOT EXISTS 사용)
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS ParsedTransactionEntity (
                id TEXT NOT NULL PRIMARY KEY,
                amount REAL NOT NULL,
                merchantName TEXT NOT NULL,
                date TEXT NOT NULL,
                rawNotification TEXT NOT NULL,
                isProcessed INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
            0
        )

        return driver
    }
}