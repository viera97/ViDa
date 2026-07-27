package com.vida.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the [MIGRATION_13_14] object is correctly constructed.
 *
 * A full data-preservation test (createDb at v13, run migration, assert schema
 * matches v14) requires `MigrationTestHelper` + `InstrumentationRegistry` +
 * the generated `13.json` / `14.json` schema exports. The no-op migration body
 * means there is no SQL to validate; the column already stored TEXT codes via
 * `Converters.fromCurrency`. End-to-end behavior is covered by the DAO and
 * mapper tests, which already exercise the v14 schema through `AppDatabase`.
 */
class Migration1314Test {

    @Test
    fun `migration covers version 13 to 14`() {
        assertEquals(13, MIGRATION_13_14.startVersion)
        assertEquals(14, MIGRATION_13_14.endVersion)
    }
}
