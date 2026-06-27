package com.vida.feature.home

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.feature.home.home.formatHomeMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for [com.vida.feature.home.home.formatHomeMoney].
 *
 * Plain JUnit 4, pure JVM. Asserts that the home dashboard always renders the
 * ISO currency code (CUP / USD / MLC / EUR) after the amount — never the
 * `$` / `€` symbol — so the four currencies that share the peso glyph stay
 * visually distinguishable.
 */
class HomeMoneyFormatterTest {

    @Test
    fun `CUP renders code not peso symbol`() {
        val result = formatHomeMoney(Money(BigDecimal("1250.50"), Currency.CUP))
        assertEquals("1,250.50 CUP", result)
        assertFalse("CUP output must not contain '\$'", result.contains('$'))
    }

    @Test
    fun `USD renders code not peso symbol`() {
        val result = formatHomeMoney(Money(BigDecimal("99.00"), Currency.USD))
        assertEquals("99.00 USD", result)
        assertFalse("USD output must not contain '\$'", result.contains('$'))
    }

    @Test
    fun `MLC renders code not peso symbol`() {
        val result = formatHomeMoney(Money(BigDecimal("250.00"), Currency.MLC))
        assertEquals("250.00 MLC", result)
        assertFalse("MLC output must not contain '\$'", result.contains('$'))
    }

    @Test
    fun `EUR renders code not euro symbol`() {
        val result = formatHomeMoney(Money(BigDecimal("10.00"), Currency.EUR))
        assertEquals("10.00 EUR", result)
        assertFalse("EUR output must not contain '€'", result.contains('€'))
    }

    @Test
    fun `zero amount still renders two fraction digits and currency code`() {
        assertEquals("0.00 CUP", formatHomeMoney(Money(BigDecimal.ZERO, Currency.CUP)))
        assertEquals("0.00 USD", formatHomeMoney(Money(BigDecimal.ZERO, Currency.USD)))
        assertEquals("0.00 MLC", formatHomeMoney(Money(BigDecimal.ZERO, Currency.MLC)))
        assertEquals("0.00 EUR", formatHomeMoney(Money(BigDecimal.ZERO, Currency.EUR)))
    }

    @Test
    fun `large amount uses es-CU thousands separator`() {
        // es-CU (Java CLDR) groups thousands with ',' and decimals with '.'
        val result = formatHomeMoney(Money(BigDecimal("1234567.89"), Currency.CUP))
        assertEquals("1,234,567.89 CUP", result)
        assertTrue("Must end with the CUP code", result.endsWith(" CUP"))
    }

    @Test
    fun `integer-looking amount is padded to two fraction digits`() {
        val result = formatHomeMoney(Money(BigDecimal("100"), Currency.USD))
        assertEquals("100.00 USD", result)
    }
}
