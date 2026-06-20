package com.vida.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class MoneyTest {

    private val oneUSD: Money = Money(BigDecimal.ONE, Currency.USD)
    private val twoUSD: Money = Money(BigDecimal("2"), Currency.USD)
    private val threeUSD: Money = Money(BigDecimal("3"), Currency.USD)
    private val oneCUP: Money = Money(BigDecimal.ONE, Currency.CUP)

    @Test
    fun `plus adds amounts in same currency`() {
        assertEquals(threeUSD, oneUSD + twoUSD)
    }

    @Test
    fun `plus throws on currency mismatch`() {
        assertThrows(IllegalArgumentException::class.java) { oneUSD + oneCUP }
    }

    @Test
    fun `minus subtracts amounts in same currency`() {
        assertEquals(oneUSD, twoUSD - oneUSD)
    }

    @Test
    fun `times multiplies by BigDecimal`() {
        assertEquals(Money(BigDecimal("2.50"), Currency.USD), oneUSD * BigDecimal("2.50"))
    }

    @Test
    fun `times multiplies by Int`() {
        assertEquals(threeUSD, oneUSD * 3)
    }

    @Test
    fun `div divides by BigDecimal`() {
        val half = twoUSD / BigDecimal("4")
        // 2 / 4 = 0.5 with HALF_EVEN rounding; result is scaled to 10
        assertEquals(BigDecimal("0.5000000000"), half.amount)
        assertEquals(Currency.USD, half.currency)
    }

    @Test
    fun `div throws on zero divisor`() {
        assertThrows(IllegalArgumentException::class.java) { oneUSD / BigDecimal.ZERO }
    }

    @Test
    fun `compareTo returns sign of amount difference`() {
        assertTrue(oneUSD < twoUSD)
        assertTrue(twoUSD > oneUSD)
        assertEquals(0, oneUSD.compareTo(oneUSD))
    }

    @Test
    fun `compareTo throws on currency mismatch`() {
        assertThrows(IllegalArgumentException::class.java) { oneUSD.compareTo(oneCUP) }
    }

    @Test
    fun `convertTo multiplies by positive rate with HALF_EVEN rounding to scale 2`() {
        val converted = oneUSD.convertTo(Currency.CUP, BigDecimal("420.123456"))
        // 1 * 420.123456 = 420.123456 → HALF_EVEN scale 2 → 420.12
        assertEquals(BigDecimal("420.12").setScale(2, RoundingMode.HALF_EVEN), converted.amount)
        assertEquals(Currency.CUP, converted.currency)
    }

    @Test
    fun `convertTo short-circuits when target equals currency`() {
        val converted = oneUSD.convertTo(Currency.USD, BigDecimal("420"))
        assertEquals(oneUSD, converted)
    }

    @Test
    fun `convertTo throws on non-positive rate`() {
        assertThrows(IllegalArgumentException::class.java) {
            oneUSD.convertTo(Currency.CUP, BigDecimal.ZERO)
        }
        assertThrows(IllegalArgumentException::class.java) {
            oneUSD.convertTo(Currency.CUP, BigDecimal("-1"))
        }
    }

    @Test
    fun `isPositive isPositive isNegative isZero classify amount sign`() {
        val pos = Money(BigDecimal("0.01"), Currency.USD)
        val neg = Money(BigDecimal("-0.01"), Currency.USD)
        val zero = Money(BigDecimal.ZERO, Currency.USD)
        assertTrue(pos.isPositive())
        assertFalse(pos.isNegative())
        assertFalse(pos.isZero())
        assertTrue(neg.isNegative())
        assertTrue(zero.isZero())
    }

    @Test
    fun `unaryMinus negates the amount`() {
        val neg = -oneUSD
        assertEquals(BigDecimal("-1"), neg.amount)
        assertEquals(Currency.USD, neg.currency)
    }

    @Test
    fun `of factory parses string amount`() {
        val money = Money.of("420.50", Currency.USD)
        assertEquals(BigDecimal("420.50"), money.amount)
        assertEquals(Currency.USD, money.currency)
    }

    @Test
    fun `ZERO constants are zero in their currency`() {
        assertEquals(BigDecimal.ZERO, Money.ZERO_CUP.amount)
        assertEquals(Currency.CUP, Money.ZERO_CUP.currency)
        assertEquals(BigDecimal.ZERO, Money.ZERO_USD.amount)
        assertEquals(Currency.USD, Money.ZERO_USD.currency)
        assertEquals(BigDecimal.ZERO, Money.ZERO_MLC.amount)
        assertEquals(Currency.MLC, Money.ZERO_MLC.currency)
    }

    @Test
    fun `data class equality uses amount and currency`() {
        assertEquals(Money(BigDecimal("1"), Currency.USD), Money(BigDecimal("1"), Currency.USD))
        assertNotEquals(Money(BigDecimal("1"), Currency.USD), Money(BigDecimal("1"), Currency.CUP))
    }
}