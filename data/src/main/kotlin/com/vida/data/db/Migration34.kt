package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Additive-only migration from schema v3 (transfers, recurring_expenses) to v4
 * (adds `name` column to wallets). Does not alter any other table.
 * See SCN-WLT-031.
 *
 * The column default ensures existing singleton wallet rows get a valid name
 * automatically. The DDL matches the column definition Room generates from the
 * v4 WalletEntity.
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE wallets ADD COLUMN name TEXT NOT NULL DEFAULT 'Billetera'",
        )
    }
}
