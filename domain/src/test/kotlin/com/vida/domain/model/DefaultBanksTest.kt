package com.vida.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultBanksTest {

    @Test
    fun `ALL contains exactly three banks`() {
        assertEquals(3, DefaultBanks.ALL.size)
    }

    @Test
    fun `BANDEC has correct properties`() {
        assertEquals("Bandec", DefaultBanks.BANDEC.name)
        assertEquals(0xFF8E0509.toInt(), DefaultBanks.BANDEC.color)
        assertTrue(DefaultBanks.BANDEC.isSystem)
    }

    @Test
    fun `BPA has correct properties`() {
        assertEquals("BPA", DefaultBanks.BPA.name)
        assertEquals(0xFFBCD1DA.toInt(), DefaultBanks.BPA.color)
        assertTrue(DefaultBanks.BPA.isSystem)
    }

    @Test
    fun `METROPOLITANO has correct properties`() {
        assertEquals("Metropolitano", DefaultBanks.METROPOLITANO.name)
        assertEquals(0xFF91D506.toInt(), DefaultBanks.METROPOLITANO.color)
        assertTrue(DefaultBanks.METROPOLITANO.isSystem)
    }

    @Test
    fun `ALL list contains BANDEC BPA and METROPOLITANO`() {
        val names = DefaultBanks.ALL.map { it.name }.toSet()
        assertTrue(names.containsAll(setOf("Bandec", "BPA", "Metropolitano")))
    }
}
