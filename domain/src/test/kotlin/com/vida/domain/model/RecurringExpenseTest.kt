package com.vida.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class RecurringExpenseTest {

    private val oneCup: Money = Money(BigDecimal.ONE, Currency.CUP)
    private val start: LocalDate = LocalDate.of(2026, 1, 1)

    @Test
    fun `valid construction succeeds for monthly recurring`() {
        val r = RecurringExpense(
            amount = Money(BigDecimal("2000"), Currency.CUP),
            currency = "CUP",
            categoryId = 1L,
            sourceType = SourceType.WALLET,
            description = "Alquiler",
            frequency = Frequency.MONTHLY,
            startDate = start,
        )
        assertEquals(0L, r.id)
        assertEquals(null, r.endDate)
        assertEquals(null, r.lastGeneratedDate)
        assertEquals(true, r.isActive)
        assertEquals(null, r.sourceId)
    }

    @Test
    fun `valid construction succeeds with explicit end date`() {
        val r = RecurringExpense(
            amount = oneCup,
            currency = "CUP",
            categoryId = 1L,
            sourceType = SourceType.CARD,
            sourceId = 5L,
            description = "Netflix",
            frequency = Frequency.MONTHLY,
            startDate = start,
            endDate = LocalDate.of(2026, 12, 31),
            lastGeneratedDate = LocalDate.of(2026, 5, 1),
            isActive = false,
        )
        assertEquals(LocalDate.of(2026, 12, 31), r.endDate)
        assertEquals(LocalDate.of(2026, 5, 1), r.lastGeneratedDate)
        assertEquals(false, r.isActive)
        assertEquals(5L, r.sourceId)
    }

    @Test
    fun `non-positive amount is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = Money.ZERO_CUP,
                currency = "CUP",
                categoryId = 1L,
                sourceType = SourceType.WALLET,
                description = "X",
                frequency = Frequency.MONTHLY,
                startDate = start,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = Money(BigDecimal("-1"), Currency.CUP),
                currency = "CUP",
                categoryId = 1L,
                sourceType = SourceType.WALLET,
                description = "X",
                frequency = Frequency.MONTHLY,
                startDate = start,
            )
        }
    }

    @Test
    fun `blank description is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = oneCup,
                currency = "CUP",
                categoryId = 1L,
                sourceType = SourceType.WALLET,
                description = "  ",
                frequency = Frequency.MONTHLY,
                startDate = start,
            )
        }
    }

    @Test
    fun `non-positive categoryId is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = oneCup,
                currency = "CUP",
                categoryId = 0L,
                sourceType = SourceType.WALLET,
                description = "X",
                frequency = Frequency.MONTHLY,
                startDate = start,
            )
        }
    }

    @Test
    fun `endDate before startDate is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = oneCup,
                currency = "CUP",
                categoryId = 1L,
                sourceType = SourceType.WALLET,
                description = "X",
                frequency = Frequency.MONTHLY,
                startDate = LocalDate.of(2026, 12, 31),
                endDate = LocalDate.of(2026, 1, 1),
            )
        }
    }

    @Test
    fun `endDate equal to startDate is accepted`() {
        val r = RecurringExpense(
            amount = oneCup,
            currency = "CUP",
            categoryId = 1L,
            sourceType = SourceType.WALLET,
            description = "X",
            frequency = Frequency.MONTHLY,
            startDate = start,
            endDate = start,
        )
        assertEquals(start, r.endDate)
    }

    @Test
    fun `WALLET source with non-null sourceId is accepted (real-id wallets)`() {
        // After commit 5742918 wallets are real entities with row ids; the
        // recurring-expense form passes the wallet's id as `sourceId` and the
        // RecurringExpense must accept it.
        val r = RecurringExpense(
            amount = oneCup,
            currency = "CUP",
            categoryId = 1L,
            sourceType = SourceType.WALLET,
            sourceId = 5L,
            description = "X",
            frequency = Frequency.MONTHLY,
            startDate = start,
        )
        assertEquals(5L, r.sourceId)
    }

    @Test
    fun `CARD source with null sourceId is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = oneCup,
                currency = "CUP",
                categoryId = 1L,
                sourceType = SourceType.CARD,
                sourceId = null,
                description = "X",
                frequency = Frequency.MONTHLY,
                startDate = start,
            )
        }
    }
}
