package com.vida.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class TransferTest {

    private val now: Instant = Instant.parse("2026-06-19T12:00:00Z")
    private val oneCup: Money = Money(BigDecimal.ONE, Currency.CUP)
    private val tenCup: Money = Money(BigDecimal.TEN, Currency.CUP)
    private val oneUsd: Money = Money(BigDecimal.ONE, Currency.USD)

    @Test
    fun `valid construction succeeds for wallet-to-stash`() {
        val t = Transfer(
            fromType = SourceType.WALLET,
            fromId = 1L,
            toType = SourceType.STASH,
            toId = 5L,
            amount = tenCup,
            dateTime = now,
        )
        assertEquals(0L, t.id)
        assertEquals(null, t.note)
        assertEquals(1L, t.fromId)
        assertEquals(5L, t.toId)
    }

    @Test
    fun `valid construction succeeds for card-to-card`() {
        val t = Transfer(
            fromType = SourceType.CARD,
            fromId = 3L,
            toType = SourceType.CARD,
            toId = 7L,
            amount = oneUsd,
            dateTime = now,
            note = "top-up",
        )
        assertEquals(3L, t.fromId)
        assertEquals(7L, t.toId)
        assertEquals("top-up", t.note)
    }

    @Test
    fun `non-positive amount is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Transfer(
                fromType = SourceType.WALLET,
                fromId = 1L,
                toType = SourceType.STASH,
                toId = 5L,
                amount = Money.ZERO_CUP,
                dateTime = now,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Transfer(
                fromType = SourceType.WALLET,
                fromId = 1L,
                toType = SourceType.STASH,
                toId = 5L,
                amount = Money(BigDecimal("-1"), Currency.CUP),
                dateTime = now,
            )
        }
    }

    @Test
    fun `transfer to self is rejected at entity layer`() {
        // card 5 → card 5
        assertThrows(IllegalArgumentException::class.java) {
            Transfer(
                fromType = SourceType.CARD,
                fromId = 5L,
                toType = SourceType.CARD,
                toId = 5L,
                amount = oneCup,
                dateTime = now,
            )
        }
        // wallet 1 → wallet 1
        assertThrows(IllegalArgumentException::class.java) {
            Transfer(
                fromType = SourceType.WALLET,
                fromId = 1L,
                toType = SourceType.WALLET,
                toId = 1L,
                amount = oneCup,
                dateTime = now,
            )
        }
        // stash 5 → stash 5
        assertThrows(IllegalArgumentException::class.java) {
            Transfer(
                fromType = SourceType.STASH,
                fromId = 5L,
                toType = SourceType.STASH,
                toId = 5L,
                amount = oneCup,
                dateTime = now,
            )
        }
    }

    @Test
    fun `transfer between different sources of same type is accepted`() {
        val t = Transfer(
            fromType = SourceType.CARD,
            fromId = 3L,
            toType = SourceType.CARD,
            toId = 5L,
            amount = oneCup,
            dateTime = now,
        )
        assertEquals(3L, t.fromId)
        assertEquals(5L, t.toId)
    }
}
