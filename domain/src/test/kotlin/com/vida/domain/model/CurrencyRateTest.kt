package com.vida.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class CurrencyRateTest {

    private val now: Instant = Instant.parse("2026-06-19T12:00:00Z")

    @Test
    fun `valid construction succeeds`() {
        val r = CurrencyRate(
            fromCurrency = "USD",
            toCurrency = "CUP",
            rate = BigDecimal("420"),
            updatedAt = now,
        )
        assertEquals(0L, r.id)
        assertEquals(BigDecimal("420"), r.rate)
    }

    @Test
    fun `non-positive rate is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyRate(
                fromCurrency = "USD",
                toCurrency = "CUP",
                rate = BigDecimal.ZERO,
                updatedAt = now,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyRate(
                fromCurrency = "USD",
                toCurrency = "CUP",
                rate = BigDecimal("-1"),
                updatedAt = now,
            )
        }
    }

    @Test
    fun `fromCurrency equal to toCurrency is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyRate(
                fromCurrency = "USD",
                toCurrency = "USD",
                rate = BigDecimal.ONE,
                updatedAt = now,
            )
        }
    }
}