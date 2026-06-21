package com.vida.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the [MIGRATION_2_3] object is correctly constructed.
 *
 * The full v2 → v3 data-preservation test (createDb at v2, insert rows, run migration,
 * assert PR #1 + PR #2 data preserved and the 2 new tables exist and are empty) requires
 * `MigrationTestHelper` + `InstrumentationRegistry` + the generated `3.json` schema
 * export. That path is blocked on the same Hilt/Robolectric instrumentation setup noted
 * in PR #1's `HiltGraphSmokeTest` and PR #2's `Migration12Test`; the full test is sketched
 * below as a reference and should be enabled once `androidx.test:runner` is on the test
 * classpath. The new-table schema is already exercised end-to-end by the DAO tests
 * (`TransferDaoTest`, `RecurringExpenseDaoTest`, `BalanceDaoTest`), which build the v3
 * in-memory database and insert/query each new table.
 */
class Migration23Test {

    @Test
    fun `migration covers version 2 to 3`() {
        assertEquals(2, MIGRATION_2_3.startVersion)
        assertEquals(3, MIGRATION_2_3.endVersion)
    }
}

/*
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration23FullTest {
    private val dbName = "migration-test.db"
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.classLoader!!,
    )

    @Test
    fun `migration 2 to 3 preserves v2 data and adds new empty tables`() {
        helper.createDb(dbName, "2.json").use { db ->
            // Insert v2 data
            db.execSQL("INSERT INTO categories (name, color, icon, is_system) VALUES ('Comida', 0, NULL, 0)")
            db.execSQL("INSERT INTO wallets (id, currency) VALUES (1, 'CUP')")
        }

        helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3).use { db ->
            // v2 data preserved
            db.query("SELECT name FROM categories").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Comida", c.getString(0))
            }
            // New tables exist and are empty
            db.query("SELECT COUNT(*) FROM transfers").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM recurring_expenses").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
        }
    }
}
*/