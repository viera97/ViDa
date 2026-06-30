package com.vida.data.mapper

import com.vida.data.db.entity.TransferEntity
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Transfer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class TransferMapperTest {
    private val mapper = TransferMapper

    @Test
    fun `card to stash round trip preserves all fields`() {
        val transfer = Transfer(
            id = 10L,
            fromType = SourceType.CARD,
            fromId = 5L,
            toType = SourceType.STASH,
            toId = 3L,
            amount = Money.of("100.00", Currency.CUP),
            dateTime = Instant.ofEpochMilli(5_000_000L),
            note = "monthly move",
        )
        val entity = mapper.toEntity(transfer)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(transfer, roundTrip)
        assertEquals(5L, entity.sourceCardId)
        assertNull(entity.sourceWalletId)
        assertNull(entity.sourceStashId)
        assertEquals(3L, entity.destinationStashId)
        assertNull(entity.destinationCardId)
        assertNull(entity.destinationWalletId)
    }

    @Test
    fun `wallet to card round trip preserves all fields`() {
        val transfer = Transfer(
            id = 0L,
            fromType = SourceType.WALLET,
            fromId = 1L,
            toType = SourceType.CARD,
            toId = 7L,
            amount = Money.of("50.00", Currency.USD),
            dateTime = Instant.ofEpochMilli(6_000_000L),
            note = null,
        )
        val entity = mapper.toEntity(transfer)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(transfer, roundTrip)
        assertEquals(1L, entity.sourceWalletId)
        assertNull(entity.sourceCardId)
        assertNull(entity.sourceStashId)
        assertEquals(7L, entity.destinationCardId)
        assertNull(entity.destinationWalletId)
        assertNull(entity.destinationStashId)
    }

    @Test
    fun `card to wallet round trip preserves all fields`() {
        val transfer = Transfer(
            id = 0L,
            fromType = SourceType.CARD,
            fromId = 2L,
            toType = SourceType.WALLET,
            toId = 1L,
            amount = Money.of("200.00", Currency.CUP),
            dateTime = Instant.ofEpochMilli(7_000_000L),
            note = "withdrawal",
        )
        val entity = mapper.toEntity(transfer)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(transfer, roundTrip)
        assertEquals(2L, entity.sourceCardId)
        assertNull(entity.sourceWalletId)
        assertNull(entity.sourceStashId)
        assertEquals(1L, entity.destinationWalletId)
        assertNull(entity.destinationCardId)
        assertNull(entity.destinationStashId)
    }

    @Test
    fun `stash to stash round trip`() {
        val transfer = Transfer(
            id = 0L,
            fromType = SourceType.STASH,
            fromId = 1L,
            toType = SourceType.STASH,
            toId = 2L,
            amount = Money.of("75.50", Currency.MLC),
            dateTime = Instant.ofEpochMilli(8_000_000L),
            note = "rebalance",
        )
        val entity = mapper.toEntity(transfer)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(transfer, roundTrip)
        assertEquals(1L, entity.sourceStashId)
        assertEquals(2L, entity.destinationStashId)
    }

    @Test
    fun `amount decomposes into minor units and currency code`() {
        val transfer = Transfer(
            id = 0L,
            fromType = SourceType.WALLET,
            fromId = 1L,
            toType = SourceType.CARD,
            toId = 1L,
            amount = Money.of("12.34", Currency.CUP),
            dateTime = Instant.ofEpochMilli(0L),
            note = null,
        )
        val entity = mapper.toEntity(transfer)

        assertEquals(1234L, entity.amountMinor)
        assertEquals("CUP", entity.amountCurrency)
    }

    @Test
    fun `wallet to card encode preserves amount across all currencies`() {
        // See note in `wallet to card encode produces source wallet id`
        // regarding the decode-side asymmetry for WALLET rows. This test
        // verifies encode-side amount decomposition for every currency.
        for (currency in Currency.values()) {
            val transfer = Transfer(
                id = 0L,
                fromType = SourceType.WALLET,
                fromId = 1L,
                toType = SourceType.CARD,
                toId = 1L,
                amount = Money.of("99.99", currency),
                dateTime = Instant.ofEpochMilli(1_000L),
                note = null,
            )
            val entity = mapper.toEntity(transfer)
            assertEquals(9999L, entity.amountMinor)
            assertEquals(currency.code, entity.amountCurrency)
        }
    }
}