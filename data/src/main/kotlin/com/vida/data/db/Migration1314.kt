package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema v13 to v14: no-op.
 *
 * `CardEntity.currency` changes from `Currency` enum to `String` (plain TEXT).
 * Since `Converters.fromCurrency` already serialized enum values as their `code`
 * (e.g. "CUP", "USD", "EUR") and the column stored TEXT via the converter, the
 * on-disk data is already string codes — no ALTER TABLE or data transform is
 * needed. This migration object exists solely to satisfy Room's schema validation
 * (Room requires a migration object for every version bump when
 * `exportSchema = true`).
 */
val MIGRATION_13_14: Migration = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No-op: column already stores TEXT codes via Converters.fromCurrency.
        // Only the mapped Kotlin type changed (Currency → String).
    }
}
