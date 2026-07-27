package com.vida.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the [MIGRATION_11_12] object is correctly constructed.
 *
 * A full data-preservation test (createDb at v11, run migration, assert banks
 * are seeded) requires `MigrationTestHelper` + `InstrumentationRegistry` +
 * the generated `12.json` schema export. That path is blocked on the same
 * Hilt/Robolectric instrumentation setup noted in `Migration12Test`. The DAO
 * tests already verify the banks table schema end-to-end.
 */
class Migration1112Test {

    @Test
    fun `migration covers version 11 to 12`() {
        assertEquals(11, MIGRATION_11_12.startVersion)
        assertEquals(12, MIGRATION_11_12.endVersion)
    }
}
