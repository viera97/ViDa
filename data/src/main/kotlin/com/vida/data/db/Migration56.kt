package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema v5 to v6: adds initial_balance_minor and
 * initial_balance_currency columns to wallets and cards tables.
 */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE wallets ADD COLUMN initial_balance_minor INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE wallets ADD COLUMN initial_balance_currency TEXT NOT NULL DEFAULT 'CUP'")
        db.execSQL("ALTER TABLE cards ADD COLUMN initial_balance_minor INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE cards ADD COLUMN initial_balance_currency TEXT NOT NULL DEFAULT 'CUP'")
    }
}
