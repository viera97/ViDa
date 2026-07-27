package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema v12 to v13: adds the `currencies` table for
 * currency management.
 *
 * System currencies are seeded during migration so that upgrading users
 * already have the defaults. `INSERT OR IGNORE` + the unique index on `code`
 * makes this idempotent: safe for fresh installs, re-runs, and users who
 * already have currencies.
 */
val MIGRATION_12_13: Migration = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `currencies` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `code` TEXT NOT NULL,
                `is_system` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_currencies_code` ON `currencies` (`code`)")

        db.execSQL("""
            INSERT OR IGNORE INTO `currencies` (`name`, `code`, `is_system`)
            VALUES ('Peso cubano', 'CUP', 1)
        """.trimIndent())
        db.execSQL("""
            INSERT OR IGNORE INTO `currencies` (`name`, `code`, `is_system`)
            VALUES ('Dólar', 'USD', 1)
        """.trimIndent())
        db.execSQL("""
            INSERT OR IGNORE INTO `currencies` (`name`, `code`, `is_system`)
            VALUES ('Moneda libremente convertible', 'MLC', 1)
        """.trimIndent())
        db.execSQL("""
            INSERT OR IGNORE INTO `currencies` (`name`, `code`, `is_system`)
            VALUES ('Euro', 'EUR', 1)
        """.trimIndent())
    }
}
