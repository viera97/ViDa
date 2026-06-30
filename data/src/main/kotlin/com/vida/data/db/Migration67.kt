package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema v6 to v7: renames `initial_balance_minor` to `balance_minor`
 * on the `wallets` and `cards` tables.
 *
 * The data in the renamed column is preserved by SQLite's `ALTER TABLE RENAME COLUMN`
 * (added in SQLite 3.25.0; Room requires SQLite ≥ 3.28 via the bundled native library).
 * No row rewrite happens — only the column name in the schema changes.
 *
 * Background: prior to this migration, the wallet/card balance shown in the UI was
 * computed by the BalanceDao SQL formula
 * `balance_displayed = initial_balance + transfers_in - transfers_out - expenses_out`,
 * which made editing the "Balance" field in the UI confusing: the field actually
 * controlled a fixed offset, not the displayed value. After this migration the
 * stored column is simply called `balance_minor` and is the value displayed in the
 * UI directly (transfers still record normally but no longer auto-update balances —
 * the user maintains the balance manually).
 */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE wallets RENAME COLUMN initial_balance_minor TO balance_minor")
        db.execSQL("ALTER TABLE cards RENAME COLUMN initial_balance_minor TO balance_minor")
    }
}