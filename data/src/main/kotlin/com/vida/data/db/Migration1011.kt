package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema v10 to v11: adds the `banks` table for
 * bank management.
 */
val MIGRATION_10_11: Migration = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `banks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `color` INTEGER NOT NULL,
                `is_system` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_banks_name` ON `banks` (`name`)")
    }
}
