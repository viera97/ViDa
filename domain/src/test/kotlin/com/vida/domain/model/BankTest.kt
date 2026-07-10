package com.vida.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BankTest {

    @Test
    fun `creates Bank with all fields`() {
        val bank = Bank(id = 1L, name = "Bandec", color = 0xFF8E0509.toInt(), isSystem = true)
        assertEquals(1L, bank.id)
        assertEquals("Bandec", bank.name)
        assertEquals(0xFF8E0509.toInt(), bank.color)
        assertEquals(true, bank.isSystem)
    }

    @Test
    fun `uses default values for id and isSystem`() {
        val bank = Bank(name = "Test", color = 0)
        assertEquals(0L, bank.id)
        assertFalse(bank.isSystem)
    }

    @Test
    fun `throws on blank name`() {
        assertThrows(IllegalArgumentException::class.java) {
            Bank(name = "  ", color = 0)
        }
    }

    @Test
    fun `throws on name longer than 50 chars`() {
        assertThrows(IllegalArgumentException::class.java) {
            Bank(name = "A".repeat(51), color = 0)
        }
    }

    @Test
    fun `accepts name of exactly 50 chars`() {
        val bank = Bank(name = "B".repeat(50), color = 0)
        assertEquals(50, bank.name.length)
    }
}
