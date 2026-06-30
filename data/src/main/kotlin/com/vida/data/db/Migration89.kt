package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema v8 to v9: adds the `provider` column to `currency_rates`
 * so users can track the source of each exchange rate (e.g. "Manual", "Banco Central").
 *
 * Existing rows get 'Manual' as the default value.
 */
val MIGRATION_8_9: Migration = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE `currency_rates`
            ADD COLUMN `provider` TEXT NOT NULL DEFAULT 'Manual'
            """.trimIndent(),
        )
    }
}
