package com.woojin.paymanagement.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = try {
            NativeSqliteDriver(
                schema = PayManagementDatabase.Schema,
                name = "PayManagementDatabase.db",
                onConfiguration = { config ->
                    config.copy(
                        version = 8
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
                        version = 8
                    )
                }
            )
        }

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
            0,
            null
        )

        // CategoryEntity 테이블이 없으면 생성
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS CategoryEntity (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                emoji TEXT NOT NULL,
                type TEXT NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1,
                sortOrder INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            0,
            null
        )

        // BalanceCardEntity 테이블이 없으면 생성
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS BalanceCardEntity (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                initialAmount REAL NOT NULL,
                currentBalance REAL NOT NULL,
                createdDate TEXT NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent(),
            0,
            null
        )

        // GiftCardEntity 테이블이 없으면 생성
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS GiftCardEntity (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                totalAmount REAL NOT NULL,
                usedAmount REAL NOT NULL,
                createdDate TEXT NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1,
                minimumUsageRate REAL NOT NULL DEFAULT 0.8
            )
            """.trimIndent(),
            0,
            null
        )

        // BudgetPlanEntity 테이블이 없으면 생성
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS BudgetPlanEntity (
                id TEXT NOT NULL PRIMARY KEY,
                periodStartDate TEXT NOT NULL,
                periodEndDate TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                UNIQUE(periodStartDate, periodEndDate)
            )
            """.trimIndent(),
            0,
            null
        )

        // CategoryBudgetEntity 마이그레이션: categoryId -> categoryIds
        // 기존 테이블 확인
        val hasOldSchema = try {
            driver.executeQuery(
                null,
                "PRAGMA table_info(CategoryBudgetEntity)",
                { cursor ->
                    var hasOldColumn = false
                    while (cursor.next().value) {
                        val columnName = cursor.getString(1)
                        if (columnName == "categoryId") {
                            hasOldColumn = true
                            break
                        }
                    }
                    app.cash.sqldelight.db.QueryResult.Value(hasOldColumn)
                },
                0
            ).value
        } catch (e: Exception) {
            false
        }

        if (hasOldSchema) {
            // 구 스키마가 존재하면 마이그레이션
            // 1. 임시 테이블 생성
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS CategoryBudgetEntity_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    budgetPlanId TEXT NOT NULL,
                    categoryIds TEXT NOT NULL,
                    categoryName TEXT NOT NULL,
                    categoryEmoji TEXT NOT NULL,
                    allocatedAmount REAL NOT NULL,
                    FOREIGN KEY (budgetPlanId) REFERENCES BudgetPlanEntity(id) ON DELETE CASCADE
                )
                """.trimIndent(),
                0,
                null
            )

            // 2. 기존 데이터를 변환하여 복사 (categoryId -> ["categoryId"] JSON 배열)
            driver.execute(
                null,
                """
                INSERT INTO CategoryBudgetEntity_new (id, budgetPlanId, categoryIds, categoryName, categoryEmoji, allocatedAmount)
                SELECT id, budgetPlanId, '["' || categoryId || '"]', categoryName, categoryEmoji, allocatedAmount
                FROM CategoryBudgetEntity
                """.trimIndent(),
                0,
                null
            )

            // 3. 구 테이블 삭제
            driver.execute(null, "DROP TABLE CategoryBudgetEntity", 0, null)

            // 4. 새 테이블 이름 변경
            driver.execute(null, "ALTER TABLE CategoryBudgetEntity_new RENAME TO CategoryBudgetEntity", 0, null)
        } else {
            // 새로 설치하는 경우 바로 새 스키마로 생성
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS CategoryBudgetEntity (
                    id TEXT NOT NULL PRIMARY KEY,
                    budgetPlanId TEXT NOT NULL,
                    categoryIds TEXT NOT NULL,
                    categoryName TEXT NOT NULL,
                    categoryEmoji TEXT NOT NULL,
                    allocatedAmount REAL NOT NULL,
                    FOREIGN KEY (budgetPlanId) REFERENCES BudgetPlanEntity(id) ON DELETE CASCADE
                )
                """.trimIndent(),
                0,
                null
            )
        }

        return driver
    }
}