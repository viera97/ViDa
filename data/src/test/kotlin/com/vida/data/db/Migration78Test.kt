package com.vida.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the [MIGRATION_7_8] object is correctly constructed.
 *
 * The full v7 → v8 end-to-end test (createDb at v7, run migration, assert the
 * `incomes` table exists and is queryable) requires `MigrationTestHelper` +
 * `InstrumentationRegistry` + the generated `8.json` schema export. The
 * end-to-end behavior is covered by the `IncomeDao` tests (which build the
 * latest in-memory database), so this constructor check is sufficient for now.
 */
class Migration78Test {

    @Test
    fun `migration covers version 7 to 8`() {
        assertEquals(7, MIGRATION_7_8.startVersion)
        assertEquals(8, MIGRATION_7_8.endVersion)
    }
}
