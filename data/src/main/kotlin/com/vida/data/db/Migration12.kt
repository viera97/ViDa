package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Additive-only migration from schema v1 (cards, stashes, wallets) to v2
 * (adds categories, expenses, refunds, currency_rates). Does not alter any
 * PR #1 table. See SCN-DATA-PR2-010.
 *
 * The DDL matches the schema Room generates from the v2 `@Entity` definitions so
 * `MigrationTestHelper` schema validation and Room's open-time integrity check agree.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // categories
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `color` INTEGER NOT NULL,
                `icon` TEXT,
                `is_system` INTEGER NOT NULL
            )
            """.trimIndent(),
        )

        // expenses (polymorphic source triplet, FK to categories/cards/stashes)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expenses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `category_id` INTEGER NOT NULL,
                `amount_minor` INTEGER NOT NULL,
                `amount_currency` TEXT NOT NULL,
                `real_amount_minor` INTEGER,
                `real_amount_currency` TEXT,
                `description` TEXT NOT NULL,
                `date_time` INTEGER NOT NULL,
                `note` TEXT,
                `source_wallet_id` INTEGER,
                `source_card_id` INTEGER,
                `source_stash_id` INTEGER,
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`source_card_id`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`source_stash_id`) REFERENCES `stashes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_expenses_category_id` ON `expenses` (`category_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_expenses_date_time` ON `expenses` (`date_time`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_expenses_source_card_id` ON `expenses` (`source_card_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_expenses_source_stash_id` ON `expenses` (`source_stash_id`)")

        // refunds (UNIQUE on original_expense_id, CASCADE from expenses)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `refunds` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `original_expense_id` INTEGER NOT NULL,
                `amount_minor` INTEGER NOT NULL,
                `amount_currency` TEXT NOT NULL,
                `reason` TEXT NOT NULL,
                `date_time` INTEGER NOT NULL,
                `note` TEXT,
                FOREIGN KEY(`original_expense_id`) REFERENCES `expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `idx_refunds_original_expense_id` ON `refunds` (`original_expense_id`)",
        )

        // currency_rates (composite index for the getRate hot path)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `currency_rates` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `from_currency` TEXT NOT NULL,
                `to_currency` TEXT NOT NULL,
                `rate` REAL NOT NULL,
                `effective_date` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_currency_rates_lookup` ON `currency_rates` (`from_currency`, `to_currency`, `effective_date`)",
        )
    }
}
