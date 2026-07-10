package com.vida.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the [MIGRATION_10_11] object is correctly constructed.
 */
class Migration1011Test {

    @Test
    fun `migration covers version 10 to 11`() {
        assertEquals(10, MIGRATION_10_11.startVersion)
        assertEquals(11, MIGRATION_10_11.endVersion)
    }
}
