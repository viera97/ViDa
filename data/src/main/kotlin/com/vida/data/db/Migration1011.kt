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

        // Seed default banks for users upgrading from v10 — `AppDatabaseCallback.onCreate`
        // only fires on fresh install, so existing users would get an empty banks table
        // and see only "Bandec" + "Otros" in the card creation dialog.
        db.execSQL("""
            INSERT OR IGNORE INTO `banks` (`name`, `color`, `is_system`)
            VALUES ('Bandec', ${0xFF8E0509.toInt()}, 1)
        """.trimIndent())
        db.execSQL("""
            INSERT OR IGNORE INTO `banks` (`name`, `color`, `is_system`)
            VALUES ('BPA', ${0xFFBCD1DA.toInt()}, 1)
        """.trimIndent())
        db.execSQL("""
            INSERT OR IGNORE INTO `banks` (`name`, `color`, `is_system`)
            VALUES ('Metropolitano', ${0xFF91D506.toInt()}, 1)
        """.trimIndent())
    }
}
