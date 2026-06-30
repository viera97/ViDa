package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema v9 to v10: adds the `recurring_incomes` table for
 * recurring income templates.
 *
 * Mirrors `recurring_expenses` but without the `category_id` column and
 * without the foreign key to the categories table — incomes are not categorized.
 */
val MIGRATION_9_10: Migration = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recurring_incomes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `amount_minor` INTEGER NOT NULL,
                `amount_currency` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `frequency` TEXT NOT NULL,
                `start_date` INTEGER NOT NULL,
                `end_date` INTEGER,
                `last_generated_date` INTEGER,
                `is_active` INTEGER NOT NULL,
                `source_wallet_id` INTEGER,
                `source_card_id` INTEGER,
                `source_stash_id` INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_recurring_incomes_source_card_id` ON `recurring_incomes` (`source_card_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_recurring_incomes_source_stash_id` ON `recurring_incomes` (`source_stash_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_recurring_incomes_is_active` ON `recurring_incomes` (`is_active`)")
    }
}
