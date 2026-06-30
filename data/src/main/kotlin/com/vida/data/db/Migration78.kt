package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema v7 to v8: adds the `incomes` table for recording
 * money received into a wallet, card, or stash.
 *
 * Mirrors `expenses` but drops `category_id`, `real_amount_*`, and the wallet
 * foreign key (wallet destination is a `destination_wallet_id` column without
 * a FK constraint — same pattern as expense's source-side wallet column).
 *
 * Indexes match the columns queried in [com.vida.data.db.dao.IncomeDao].
 * Auto-update of source balances (Option C) happens in
 * [com.vida.data.repository.IncomeRepositoryImpl.upsert] — the migration only
 * creates the ledger table, not the derived-balance side.
 */
val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `incomes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `amount_minor` INTEGER NOT NULL,
                `amount_currency` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `date_time` INTEGER NOT NULL,
                `note` TEXT,
                `destination_wallet_id` INTEGER,
                `destination_card_id` INTEGER,
                `destination_stash_id` INTEGER,
                FOREIGN KEY(`destination_card_id`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`destination_stash_id`) REFERENCES `stashes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_incomes_date_time` ON `incomes` (`date_time`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_incomes_destination_card_id` ON `incomes` (`destination_card_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_incomes_destination_stash_id` ON `incomes` (`destination_stash_id`)")
    }
}
