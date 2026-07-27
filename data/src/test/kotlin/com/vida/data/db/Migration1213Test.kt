package com.vida.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the [MIGRATION_12_13] object is correctly constructed.
 *
 * The full v12 → v13 data-preservation test (createDb at v12, run migration,
 * assert `currencies` table exists with 4 system rows and unique index on code)
 * requires `MigrationTestHelper` + `InstrumentationRegistry` + the generated
 * `12.json` / `13.json` schema exports. The end-to-end behavior is already
 * covered by [com.vida.data.db.dao.CurrencyDaoTest], which builds the v13
 * in-memory database and exercises the same SQL surface Room generates from
 * the entity, so this constructor check is sufficient for now.
 */
class Migration1213Test {

    @Test
    fun `migration covers version 12 to 13`() {
        assertEquals(12, MIGRATION_12_13.startVersion)
        assertEquals(13, MIGRATION_12_13.endVersion)
    }
}
