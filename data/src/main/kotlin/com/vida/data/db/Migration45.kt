package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema v4 to v5: recreates the `wallets` table with
 * AUTOINCREMENT PK for multi-wallet support.
 *
 * The DDL matches the schema Room generates from the v5 WalletEntity so
 * `MigrationTestHelper` schema validation and Room's open-time integrity check agree.
 */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recreate wallets table with AUTOINCREMENT
        db.execSQL("ALTER TABLE wallets RENAME TO wallets_old")
        db.execSQL(
            """
            CREATE TABLE wallets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                currency TEXT NOT NULL,
                name TEXT NOT NULL DEFAULT 'Billetera'
            )
            """.trimIndent(),
        )
        db.execSQL("INSERT INTO wallets (id, currency, name) SELECT id, currency, name FROM wallets_old")
        db.execSQL("DROP TABLE wallets_old")
    }
}
