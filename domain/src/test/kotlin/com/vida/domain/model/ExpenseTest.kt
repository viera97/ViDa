package com.vida.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class ExpenseTest {

    private val now: Instant = Instant.parse("2026-06-19T12:00:00Z")

    private fun cup(amount: String): Money = Money(BigDecimal(amount), Currency.CUP)

    @Test
    fun `valid construction succeeds for minimal required fields`() {
        val e = Expense(
            categoryId = 1L,
            amount = cup("100"),
            description = "Almuerzo",
            dateTime = now,
            sourceType = SourceType.WALLET,
        )
        assertEquals(0L, e.id)
        assertEquals(null, e.realAmount)
        assertEquals(null, e.sourceId)
        assertEquals(null, e.note)
    }

    @Test
    fun `non-positive amount is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                categoryId = 1L,
                amount = cup("0"),
                description = "X",
                dateTime = now,
                sourceType = SourceType.WALLET,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                categoryId = 1L,
                amount = cup("-10"),
                description = "X",
                dateTime = now,
                sourceType = SourceType.WALLET,
            )
        }
    }

    @Test
    fun `blank description is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                categoryId = 1L,
                amount = cup("10"),
                description = "  ",
                dateTime = now,
                sourceType = SourceType.WALLET,
            )
        }
    }

    @Test
    fun `non-positive categoryId is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                categoryId = 0L,
                amount = cup("10"),
                description = "X",
                dateTime = now,
                sourceType = SourceType.WALLET,
            )
        }
    }

    @Test
    fun `realAmount in a different currency from amount is rejected`() {
        val usd = Money(BigDecimal("5"), Currency.USD)
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                categoryId = 1L,
                amount = cup("100"),
                realAmount = usd,
                description = "X",
                dateTime = now,
                sourceType = SourceType.WALLET,
            )
        }
    }

    @Test
    fun `realAmount in same currency is accepted`() {
        val e = Expense(
            categoryId = 1L,
            amount = cup("100"),
            realAmount = cup("95"),
            description = "X",
            dateTime = now,
            sourceType = SourceType.WALLET,
        )
        assertEquals(cup("95"), e.realAmount)
    }

    @Test
    fun `WALLET source with non-null sourceId is accepted (real-id wallets)`() {
        // After commit 5742918 wallets are real entities with row ids; the
        // form passes the wallet's id as `sourceId` and the Expense must accept it.
        val e = Expense(
            categoryId = 1L,
            amount = cup("10"),
            description = "X",
            dateTime = now,
            sourceType = SourceType.WALLET,
            sourceId = 5L,
        )
        assertEquals(5L, e.sourceId)
    }

    @Test
    fun `CARD source requires non-null sourceId`() {
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                categoryId = 1L,
                amount = cup("10"),
                description = "X",
                dateTime = now,
                sourceType = SourceType.CARD,
                sourceId = null,
            )
        }
        // accepted case
        val e = Expense(
            categoryId = 1L,
            amount = cup("10"),
            description = "X",
            dateTime = now,
            sourceType = SourceType.CARD,
            sourceId = 5L,
        )
        assertEquals(5L, e.sourceId)
    }

    @Test
    fun `STASH source requires non-null sourceId`() {
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                categoryId = 1L,
                amount = cup("10"),
                description = "X",
                dateTime = now,
                sourceType = SourceType.STASH,
                sourceId = null,
            )
        }
    }
}