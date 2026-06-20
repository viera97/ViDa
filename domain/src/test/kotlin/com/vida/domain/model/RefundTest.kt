package com.vida.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class RefundTest {

    private val now: Instant = Instant.parse("2026-06-19T12:00:00Z")
    private val oneCup: Money = Money(BigDecimal.ONE, Currency.CUP)

    @Test
    fun `valid construction succeeds`() {
        val r = Refund(
            originalExpenseId = 42L,
            amount = oneCup,
            reason = "defective product",
            dateTime = now,
        )
        assertEquals(0L, r.id)
        assertEquals(null, r.note)
    }

    @Test
    fun `non-positive amount is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Refund(
                originalExpenseId = 42L,
                amount = Money(BigDecimal.ZERO, Currency.CUP),
                reason = "X",
                dateTime = now,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Refund(
                originalExpenseId = 42L,
                amount = Money(BigDecimal("-1"), Currency.CUP),
                reason = "X",
                dateTime = now,
            )
        }
    }

    @Test
    fun `blank reason is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Refund(
                originalExpenseId = 42L,
                amount = oneCup,
                reason = "   ",
                dateTime = now,
            )
        }
    }

    @Test
    fun `non-positive originalExpenseId is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Refund(
                originalExpenseId = 0L,
                amount = oneCup,
                reason = "X",
                dateTime = now,
            )
        }
    }
}