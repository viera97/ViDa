package com.vida.data.mapper.util

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyMapperUtilTest {

    @Test
    fun `toColumns decomposes money into minor units and currency code`() {
        val money = Money.of("12.34", Currency.CUP)
        val (minor, code) = money.toColumns()
        assertEquals(1234L, minor)
        assertEquals("CUP", code)
    }

    @Test
    fun `toColumns handles zero`() {
        val money = Money(BigDecimal.ZERO, Currency.USD)
        val (minor, code) = money.toColumns()
        assertEquals(0L, minor)
        assertEquals("USD", code)
    }

    @Test
    fun `toColumns handles negative amount`() {
        val money = Money.of("-5.00", Currency.MLC)
        val (minor, code) = money.toColumns()
        assertEquals(-500L, minor)
        assertEquals("MLC", code)
    }

    @Test
    fun `toMoney round-trips from minor units and code`() {
        val money = (2500L to "CUP").toMoney()
        assertEquals(BigDecimal("25.00"), money.amount)
        assertEquals(Currency.CUP, money.currency)
    }

    @Test
    fun `toMoney handles zero`() {
        val money = (0L to "USD").toMoney()
        assertEquals(BigDecimal.ZERO.setScale(2), money.amount)
        assertEquals(Currency.USD, money.currency)
    }

    @Test
    fun `toMoney handles negative`() {
        val money = (-500L to "MLC").toMoney()
        assertEquals(BigDecimal("-5.00"), money.amount)
        assertEquals(Currency.MLC, money.currency)
    }

    @Test
    fun `full round-trip Money toColumns toMoney`() {
        val original = Money.of("99.99", Currency.CUP)
        val (minor, code) = original.toColumns()
        val restored = (minor to code).toMoney()
        assertEquals(original, restored)
    }
}
