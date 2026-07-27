package com.vida.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the [MIGRATION_14_15] object is correctly constructed.
 *
 * A full data-preservation test (createDb at v14, run migration, assert schema
 * matches v15) requires `MigrationTestHelper` + `InstrumentationRegistry` +
 * the generated `14.json` / `15.json` schema exports. The no-op migration body
 * means there is no SQL to validate; the column already stored TEXT codes via
 * `Converters.fromCurrency`. End-to-end behavior is covered by the DAO and
 * mapper tests, which already exercise the v15 schema through `AppDatabase`.
 */
class Migration1415Test {

    @Test
    fun `migration covers version 14 to 15`() {
        assertEquals(14, MIGRATION_14_15.startVersion)
        assertEquals(15, MIGRATION_14_15.endVersion)
    }
}
