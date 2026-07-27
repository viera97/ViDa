package com.vida.data.mapper

import com.vida.data.db.entity.CurrencyRateEntity
import com.vida.domain.model.CurrencyRate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class CurrencyRateMapperTest {
    private val mapper = CurrencyRateMapper

    @Test
    fun `round trip preserves all fields`() {
        val rate = CurrencyRate(
            id = 1L,
            fromCurrency = "USD",
            toCurrency = "CUP",
            rate = BigDecimal("24.50"),
            updatedAt = Instant.ofEpochMilli(5_000_000L),
            provider = "Manual",
        )
        val entity = mapper.toEntity(rate)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(rate.id, roundTrip.id)
        assertEquals(rate.fromCurrency, roundTrip.fromCurrency)
        assertEquals(rate.toCurrency, roundTrip.toCurrency)
        assertEquals(0, rate.rate.compareTo(roundTrip.rate))
        assertEquals(rate.updatedAt.toEpochMilli(), roundTrip.updatedAt.toEpochMilli())
    }

    @Test
    fun `updatedAt maps to effective_date column`() {
        val rate = CurrencyRate(
            id = 0L,
            fromCurrency = "USD",
            toCurrency = "MLC",
            rate = BigDecimal("1.10"),
            updatedAt = Instant.ofEpochMilli(12345L),
            provider = "Manual",
        )
        val entity = mapper.toEntity(rate)
        assertEquals(12345L, entity.effectiveDate)
    }

    @Test
    fun `rate BigDecimal round-trips via Double`() {
        val rate = CurrencyRate(
            id = 0L,
            fromCurrency = "USD",
            toCurrency = "CUP",
            rate = BigDecimal("24.50"),
            updatedAt = Instant.ofEpochMilli(0L),
        )
        val entity = mapper.toEntity(rate)
        assertEquals(24.50, entity.rate, 0.0001)

        val back = mapper.toDomain(entity)
        assertEquals(0, BigDecimal("24.50").compareTo(back.rate))
    }

    @Test
    fun `all currency pairs round trip`() {
        val pairs = listOf(
            "USD" to "CUP",
            "CUP" to "USD",
            "USD" to "MLC",
            "MLC" to "CUP",
        )
        for ((from, to) in pairs) {
            val rate = CurrencyRate(
                id = 0L,
                fromCurrency = from,
                toCurrency = to,
                rate = BigDecimal("10.00"),
                updatedAt = Instant.ofEpochMilli(1_000L),
                provider = "Manual",
            )
            val entity = mapper.toEntity(rate)
            val roundTrip = mapper.toDomain(entity)
            assertEquals(from, roundTrip.fromCurrency)
            assertEquals(to, roundTrip.toCurrency)
        }
    }
}
