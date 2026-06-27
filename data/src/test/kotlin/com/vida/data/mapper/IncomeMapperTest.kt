package com.vida.data.mapper

import com.vida.domain.model.Currency
import com.vida.domain.model.Income
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class IncomeMapperTest {
    private val mapper = IncomeMapper

    @Test
    fun `wallet destination round trip preserves all fields`() {
        val income = Income(
            id = 10L,
            amount = Money.of("5000.00", Currency.CUP),
            description = "Salario",
            dateTime = Instant.ofEpochMilli(5_000_000L),
            sourceType = SourceType.WALLET,
            sourceId = 1L,
            note = "monthly salary",
        )
        val entity = mapper.toEntity(income)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(income, roundTrip)
        assertEquals(1L, entity.destinationWalletId)
        assertNull(entity.destinationCardId)
        assertNull(entity.destinationStashId)
    }

    @Test
    fun `card destination round trip`() {
        val income = Income(
            id = 11L,
            amount = Money.of("100.00", Currency.USD),
            description = "Refund",
            dateTime = Instant.ofEpochMilli(6_000_000L),
            sourceType = SourceType.CARD,
            sourceId = 3L,
        )
        val entity = mapper.toEntity(income)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(income, roundTrip)
        assertEquals(3L, entity.destinationCardId)
        assertNull(entity.destinationWalletId)
        assertNull(entity.destinationStashId)
    }

    @Test
    fun `stash destination round trip`() {
        val income = Income(
            id = 12L,
            amount = Money.of("50.00", Currency.MLC),
            description = "Bonus",
            dateTime = Instant.ofEpochMilli(7_000_000L),
            sourceType = SourceType.STASH,
            sourceId = 4L,
            note = "extra",
        )
        val entity = mapper.toEntity(income)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(income, roundTrip)
        assertEquals(4L, entity.destinationStashId)
        assertNull(entity.destinationWalletId)
        assertNull(entity.destinationCardId)
    }

    @Test
    fun `amount decomposes into minor units and currency code`() {
        val income = Income(
            id = 0L,
            amount = Money.of("12.34", Currency.USD),
            description = "x",
            dateTime = Instant.ofEpochMilli(0L),
            sourceType = SourceType.WALLET,
            sourceId = 1L,
        )
        val entity = mapper.toEntity(income)

        assertEquals(1234L, entity.amountMinor)
        assertEquals("USD", entity.amountCurrency)
    }

    @Test
    fun `note null preserved`() {
        val income = Income(
            id = 0L,
            amount = Money.of("10.00", Currency.CUP),
            description = "x",
            dateTime = Instant.ofEpochMilli(0L),
            sourceType = SourceType.WALLET,
            sourceId = 1L,
            note = null,
        )
        val entity = mapper.toEntity(income)

        assertNull(entity.note)
    }
}
