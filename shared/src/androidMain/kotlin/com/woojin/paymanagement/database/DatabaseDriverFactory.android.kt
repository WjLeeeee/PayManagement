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
            0
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
            0
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
            0
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
            0
        )

        // TransactionEntity merchant 컬럼 추가 마이그레이션
        val merchantColumnInfo = try {
            driver.executeQuery(
                null,
                "PRAGMA table_info(TransactionEntity)",
                { cursor ->
                    var hasMerchant = false
                    var merchantPosition = -1
                    while (cursor.next().value) {
                        val position = cursor.getLong(0)?.toInt() ?: -1
                        val columnName = cursor.getString(1)
                        if (columnName == "merchant") {
                            hasMerchant = true
                            merchantPosition = position
                            break
                        }
                    }
                    app.cash.sqldelight.db.QueryResult.Value(Pair(hasMerchant, merchantPosition))
                },
                0
            ).value
        } catch (e: Exception) {
            Pair(false, -1)
        }

        val hasMerchantColumn = merchantColumnInfo.first
        val merchantPosition = merchantColumnInfo.second

        if (!hasMerchantColumn) {
            // merchant 컬럼이 없으면 테이블 재생성 (올바른 컬럼 순서로)
            try {
                driver.execute(
                    null,
                    """
                    CREATE TABLE IF NOT EXISTS TransactionEntity_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT NOT NULL,
                        merchant TEXT,
                        memo TEXT NOT NULL,
                        date TEXT NOT NULL,
                        incomeType TEXT,
                        paymentMethod TEXT,
                        balanceCardId TEXT,
                        giftCardId TEXT,
                        cardName TEXT,
                        actualAmount REAL,
                        settlementAmount REAL,
                        isSettlement INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                    0
                )

                driver.execute(
                    null,
                    """
                    INSERT INTO TransactionEntity_new
                    SELECT id, amount, type, category, NULL as merchant, memo, date, incomeType, paymentMethod,
                           balanceCardId, giftCardId, cardName, actualAmount, settlementAmount, isSettlement
                    FROM TransactionEntity
                    """.trimIndent(),
                    0
                )

                driver.execute(null, "DROP TABLE TransactionEntity", 0)
                driver.execute(null, "ALTER TABLE TransactionEntity_new RENAME TO TransactionEntity", 0)
            } catch (e: Exception) {
                // 테이블이 없거나 다른 오류 무시 (스키마가 새로 생성될 것임)
            }
        } else if (merchantPosition != 4) {
            // merchant 컬럼이 있지만 잘못된 위치에 있으면 테이블 재생성
            try {
                driver.execute(
                    null,
                    """
                    CREATE TABLE TransactionEntity_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT NOT NULL,
                        merchant TEXT,
                        memo TEXT NOT NULL,
                        date TEXT NOT NULL,
                        incomeType TEXT,
                        paymentMethod TEXT,
                        balanceCardId TEXT,
                        giftCardId TEXT,
                        cardName TEXT,
                        actualAmount REAL,
                        settlementAmount REAL,
                        isSettlement INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                    0
                )

                driver.execute(
                    null,
                    """
                    INSERT INTO TransactionEntity_new
                    SELECT id, amount, type, category, merchant, memo, date, incomeType, paymentMethod,
                           balanceCardId, giftCardId, cardName, actualAmount, settlementAmount, isSettlement
                    FROM TransactionEntity
                    """.trimIndent(),
                    0
                )

                driver.execute(null, "DROP TABLE TransactionEntity", 0)
                driver.execute(null, "ALTER TABLE TransactionEntity_new RENAME TO TransactionEntity", 0)
            } catch (e: Exception) {
                // 오류 무시
            }
        }

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
                0
            )

            // 2. 기존 데이터를 변환하여 복사 (categoryId -> ["categoryId"] JSON 배열)
            driver.execute(
                null,
                """
                INSERT INTO CategoryBudgetEntity_new (id, budgetPlanId, categoryIds, categoryName, categoryEmoji, allocatedAmount)
                SELECT id, budgetPlanId, '["' || categoryId || '"]', categoryName, categoryEmoji, allocatedAmount
                FROM CategoryBudgetEntity
                """.trimIndent(),
                0
            )

            // 3. 구 테이블 삭제
            driver.execute(null, "DROP TABLE CategoryBudgetEntity", 0)

            // 4. 새 테이블 이름 변경
            driver.execute(null, "ALTER TABLE CategoryBudgetEntity_new RENAME TO CategoryBudgetEntity", 0)
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
                0
            )
        }

        return driver
    }
}