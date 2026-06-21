package com.vida.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the [MIGRATION_1_2] object is correctly constructed.
 *
 * The full v1 → v2 data-preservation test (createDb at v1, insert rows, run migration,
 * assert PR #1 data preserved and the 4 new tables exist and are empty) requires
 * `MigrationTestHelper` + `InstrumentationRegistry` + the generated `2.json` schema
 * export. That path is blocked on the same Hilt/Robolectric instrumentation setup noted
 * in PR #1's `HiltGraphSmokeTest`; the full test is sketched below as a reference and
 * should be enabled once `androidx.test:runner` is on the test classpath. The new-table
 * schema is already exercised end-to-end by the DAO tests, which build the v2 in-memory
 * database and insert/query each new table.
 */
class Migration12Test {

    @Test
    fun `migration covers version 1 to 2`() {
        assertEquals(1, MIGRATION_1_2.startVersion)
        assertEquals(2, MIGRATION_1_2.endVersion)
    }
}

/*
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration12FullTest {
    private val dbName = "migration-test.db"
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.classLoader!!,
    )

    @Test
    fun `migration 1 to 2 preserves v1 data and adds new empty tables`() {
        helper.createDb(dbName, "1.json").use { db ->
            db.execSQL(
                "INSERT INTO cards (maskedNumber, bank, type, currency, note, expirationDate) " +
                    "VALUES ('123456******7890', 'POP', 'DEBIT', 'CUP', NULL, 20460)",
            )
            db.execSQL(
                "INSERT INTO stashes (name, createdAt, updatedAt, currency) " +
                    "VALUES ('Emergency', 1000, 1000, 'USD')",
            )
            db.execSQL("INSERT INTO wallets (id, currency) VALUES (1, 'CUP')")
        }

        helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT bank FROM cards").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("POP", c.getString(0))
            }
            db.query("SELECT COUNT(*) FROM categories").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM expenses").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM refunds").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM currency_rates").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
        }
    }
}
*/
