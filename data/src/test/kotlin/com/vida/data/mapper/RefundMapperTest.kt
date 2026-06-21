package com.vida.data.mapper

import com.vida.data.db.entity.RefundEntity
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Refund
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RefundMapperTest {
    private val mapper = RefundMapper

    @Test
    fun `round trip preserves all fields with note`() {
        val refund = Refund(
            id = 1L,
            originalExpenseId = 5L,
            amount = Money.of("25.00", Currency.USD),
            reason = "defective",
            dateTime = Instant.ofEpochMilli(5_000_000L),
            note = "returned to store",
        )
        val entity = mapper.toEntity(refund)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(refund, roundTrip)
        assertEquals(5L, entity.originalExpenseId)
        assertEquals(2500L, entity.amountMinor)
        assertEquals("USD", entity.amountCurrency)
    }

    @Test
    fun `round trip with null note`() {
        val refund = Refund(
            id = 2L,
            originalExpenseId = 6L,
            amount = Money.of("10.00", Currency.CUP),
            reason = "cancelled",
            dateTime = Instant.ofEpochMilli(6_000_000L),
            note = null,
        )
        val entity = mapper.toEntity(refund)
        val roundTrip = mapper.toDomain(refund.let { mapper.toDomain(mapper.toEntity(it)) })

        assertEquals(refund, roundTrip)
        assertEquals(null, entity.note)
    }

    @Test
    fun `amount decomposes into minor units and currency code`() {
        val refund = Refund(
            id = 0L,
            originalExpenseId = 1L,
            amount = Money.of("7.50", Currency.MLC),
            reason = "r",
            dateTime = Instant.ofEpochMilli(0L),
        )
        val entity = mapper.toEntity(refund)
        assertEquals(750L, entity.amountMinor)
        assertEquals("MLC", entity.amountCurrency)
    }
}
