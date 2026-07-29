package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema v15 to v16: add missing indexes for query performance.
 *
 * ## What changed
 *
 * ### Single-column indexes missing from entities
 * - `expenses.source_wallet_id` — the column existed but had no index,
 *   used by [ExpenseDao.observeBySource] with `sourceType = 'WALLET'`.
 * - `incomes.destination_wallet_id` — same for `IncomeDao.observeBySource`.
 * - `recurring_expenses.source_wallet_id` — missing index on the polymorphic
 *   WALLET source column.
 * - `recurring_incomes.source_wallet_id` — same.
 *
 * ### Composite indexes for the BalanceDao stash-balance hot path
 * Every stash balance subquery filters by `(source_stash_id, date_time)` or
 * `(destination_stash_id, date_time)`.  Individual indexes on each column
 * force SQLite to choose only one, causing a partial scan.  Composite indexes
 * let SQLite do a single range scan:
 *
 * - `expenses(source_stash_id, date_time)`
 * - `incomes(destination_stash_id, date_time)`
 * - `transfers(source_stash_id, date_time)`
 * - `transfers(destination_stash_id, date_time)`
 */
val MIGRATION_15_16: Migration = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ── Missing single-column indexes ────────────────────────────────
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_expenses_source_wallet_id " +
                "ON expenses(source_wallet_id)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_incomes_destination_wallet_id " +
                "ON incomes(destination_wallet_id)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_recurring_expenses_source_wallet_id " +
                "ON recurring_expenses(source_wallet_id)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_recurring_incomes_source_wallet_id " +
                "ON recurring_incomes(source_wallet_id)",
        )

        // ── Composite indexes for stash balance queries ───────────────────
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_expenses_source_stash_date " +
                "ON expenses(source_stash_id, date_time)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_incomes_dest_stash_date " +
                "ON incomes(destination_stash_id, date_time)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_transfers_source_stash_date " +
                "ON transfers(source_stash_id, date_time)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_transfers_dest_stash_date " +
                "ON transfers(destination_stash_id, date_time)",
        )
    }
}
