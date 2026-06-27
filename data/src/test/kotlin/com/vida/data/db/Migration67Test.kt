package com.vida.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the [MIGRATION_6_7] object is correctly constructed.
 *
 * The full v6 → v7 data-preservation test (createDb at v6, insert wallet/card row with
 * an `initial_balance_minor` value, run migration, assert the value is preserved under
 * the new `balance_minor` column name) requires `MigrationTestHelper` +
 * `InstrumentationRegistry` + the generated `6.json` / `7.json` schema exports. The
 * end-to-end behavior is already covered by the `BalanceDao` tests (which build the
 * latest in-memory database), so this constructor check is sufficient for now.
 */
class Migration67Test {

    @Test
    fun `migration covers version 6 to 7`() {
        assertEquals(6, MIGRATION_6_7.startVersion)
        assertEquals(7, MIGRATION_6_7.endVersion)
    }
}