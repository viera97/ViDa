package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Additive-only migration from schema v2 (cards, stashes, wallets, categories,
 * expenses, refunds, currency_rates) to v3 (adds transfers, recurring_expenses).
 * Does not alter any PR #1 or PR #2 table. See SCN-DATA-PR3-001.
 *
 * The DDL matches the schema Room generates from the v3 `@Entity` definitions so
 * `MigrationTestHelper` schema validation and Room's open-time integrity check agree.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // transfers — polymorphic source + destination triplets
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transfers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `amount_minor` INTEGER NOT NULL,
                `amount_currency` TEXT NOT NULL,
                `date_time` INTEGER NOT NULL,
                `note` TEXT,
                `source_wallet_id` INTEGER,
                `source_card_id` INTEGER,
                `source_stash_id` INTEGER,
                `destination_wallet_id` INTEGER,
                `destination_card_id` INTEGER,
                `destination_stash_id` INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_transfers_source_wallet_id` ON `transfers` (`source_wallet_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_transfers_source_card_id` ON `transfers` (`source_card_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_transfers_source_stash_id` ON `transfers` (`source_stash_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_transfers_destination_wallet_id` ON `transfers` (`destination_wallet_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_transfers_destination_card_id` ON `transfers` (`destination_card_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_transfers_destination_stash_id` ON `transfers` (`destination_stash_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_transfers_date_time` ON `transfers` (`date_time`)")

        // recurring_expenses — FK to categories RESTRICT, polymorphic source triplet
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recurring_expenses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `category_id` INTEGER NOT NULL,
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
                `source_stash_id` INTEGER,
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_recurring_expenses_category_id` ON `recurring_expenses` (`category_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_recurring_expenses_source_card_id` ON `recurring_expenses` (`source_card_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_recurring_expenses_source_stash_id` ON `recurring_expenses` (`source_stash_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_recurring_expenses_is_active` ON `recurring_expenses` (`is_active`)")
    }
}
