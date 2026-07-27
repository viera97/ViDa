package com.vida.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class CurrencyInfoTest {

    @Test
    fun `creates CurrencyInfo with all fields`() {
        val currency = CurrencyInfo(
            id = 1L,
            name = "Dólar",
            code = "USD",
            isSystem = true,
        )
        assertEquals(1L, currency.id)
        assertEquals("Dólar", currency.name)
        assertEquals("USD", currency.code)
        assertEquals(true, currency.isSystem)
    }

    @Test
    fun `uses default values for id and isSystem`() {
        val currency = CurrencyInfo(name = "Bitcoin", code = "BTC")
        assertEquals(0L, currency.id)
        assertFalse(currency.isSystem)
    }

    @Test
    fun `throws on blank name`() {
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyInfo(name = "  ", code = "USD")
        }
    }

    @Test
    fun `throws on empty name`() {
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyInfo(name = "", code = "USD")
        }
    }

    @Test
    fun `throws on name longer than 50 chars`() {
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyInfo(name = "A".repeat(51), code = "USD")
        }
    }

    @Test
    fun `accepts name of exactly 50 chars`() {
        val currency = CurrencyInfo(name = "B".repeat(50), code = "USD")
        assertEquals(50, currency.name.length)
    }

    @Test
    fun `throws on blank code`() {
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyInfo(name = "Bitcoin", code = "  ")
        }
    }

    @Test
    fun `throws on empty code`() {
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyInfo(name = "Bitcoin", code = "")
        }
    }

    @Test
    fun `throws on code longer than 10 chars`() {
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyInfo(name = "Bitcoin", code = "VERYLONGCODE")
        }
    }

    @Test
    fun `accepts code of exactly 10 chars`() {
        val currency = CurrencyInfo(name = "Ten chars", code = "ABCDEFGHIJ")
        assertEquals(10, currency.code.length)
    }

    @Test
    fun `valid name and code passes init`() {
        val currency = CurrencyInfo(name = "Bitcoin", code = "BTC")
        assertEquals("Bitcoin", currency.name)
        assertEquals("BTC", currency.code)
    }

    @Test
    fun `system flag can be explicitly set to false`() {
        val currency = CurrencyInfo(name = "Bitcoin", code = "BTC", isSystem = false)
        assertFalse(currency.isSystem)
    }
}
