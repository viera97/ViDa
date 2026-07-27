package com.vida.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema v11 to v12: seeds the default banks for users who
 * already upgraded from v10→v11 before `MIGRATION_10_11` included the seed.
 *
 * `AppDatabaseCallback.onCreate` only fires on fresh install, so upgrading
 * users would get an empty `banks` table and see only "Bandec" + "Otros" in
 * the card creation dialog (ViDa #471). This migration ensures every user has
 * the 3 default banks regardless of install path.
 *
 * `INSERT OR IGNORE` + the unique index on `name` makes this idempotent:
 * safe for fresh installs, re-runs, and users who already have banks.
 */
val MIGRATION_11_12: Migration = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            INSERT OR IGNORE INTO `banks` (`name`, `color`, `is_system`)
            VALUES ('Bandec', ${0xFF8E0509.toInt()}, 1)
        """.trimIndent())
        db.execSQL("""
            INSERT OR IGNORE INTO `banks` (`name`, `color`, `is_system`)
            VALUES ('BPA', ${0xFFBCD1DA.toInt()}, 1)
        """.trimIndent())
        db.execSQL("""
            INSERT OR IGNORE INTO `banks` (`name`, `color`, `is_system`)
            VALUES ('Metropolitano', ${0xFF91D506.toInt()}, 1)
        """.trimIndent())
    }
}
