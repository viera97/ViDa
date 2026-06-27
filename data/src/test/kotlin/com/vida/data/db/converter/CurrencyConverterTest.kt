package com.vida.data.db.converter

import com.vida.domain.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CurrencyConverterTest {

    private val converter = CurrencyConverter

    @Test
    fun `round-trips CUP`() {
        val code = converter.fromCurrency(Currency.CUP)
        assertEquals("CUP", code)
        assertEquals(Currency.CUP, converter.toCurrency(code))
    }

    @Test
    fun `round-trips USD`() {
        val code = converter.fromCurrency(Currency.USD)
        assertEquals("USD", code)
        assertEquals(Currency.USD, converter.toCurrency(code))
    }

    @Test
    fun `round-trips MLC`() {
        val code = converter.fromCurrency(Currency.MLC)
        assertEquals("MLC", code)
        assertEquals(Currency.MLC, converter.toCurrency(code))
    }

    @Test
    fun `throws on unknown currency code`() {
        assertThrows(IllegalArgumentException::class.java) {
            converter.toCurrency("XYZ")
        }
    }
}
