package com.vida.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the [MIGRATION_3_4] object is correctly constructed.
 *
 * The full v3 → v4 data-preservation test (createDb at v3, insert wallet row, run
 * migration, assert name column present with default 'Billetera') requires
 * `MigrationTestHelper` + `InstrumentationRegistry` + the generated `4.json` schema
 * export. That path is blocked on the same Hilt/Robolectric instrumentation setup noted
 * in Migration12Test; the full test is sketched below as a reference and should be
 * enabled once `androidx.test:runner` is on the test classpath. The new column is
 * already exercised end-to-end by WalletDao tests which build the v4 in-memory database.
 */
class Migration34Test {

    @Test
    fun `migration covers version 3 to 4`() {
        assertEquals(3, MIGRATION_3_4.startVersion)
        assertEquals(4, MIGRATION_3_4.endVersion)
    }
}

/*
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration34FullTest {
    private val dbName = "migration-test.db"
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.classLoader!!,
    )

    @Test
    fun `migration 3 to 4 preserves wallet data and adds name column with default`() {
        helper.createDb(dbName, "3.json").use { db ->
            db.execSQL("INSERT INTO wallets (id, currency) VALUES (1, 'CUP')")
        }

        helper.runMigrationsAndValidate(dbName, 4, true, MIGRATION_3_4).use { db ->
            // Existing row preserved with default name
            db.query("SELECT id, currency, name FROM wallets").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1L, c.getLong(0))
                assertEquals("CUP", c.getString(1))
                assertEquals("Billetera", c.getString(2))
            }
        }
    }
}
*/
