package com.vida.data.mapper

import com.vida.data.db.entity.RecurringExpenseEntity
import com.vida.domain.model.Currency
import com.vida.domain.model.Frequency
import com.vida.domain.model.Money
import com.vida.domain.model.RecurringExpense
import com.vida.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurringExpenseMapperTest {
    private val mapper = RecurringExpenseMapper

    @Test
    fun `card source round trip preserves all fields`() {
        val recurring = RecurringExpense(
            id = 10L,
            amount = Money.of("50.00", Currency.CUP),
            currency = "CUP",
            categoryId = 1L,
            sourceType = SourceType.CARD,
            sourceId = 7L,
            description = "Netflix",
            frequency = Frequency.MONTHLY,
            startDate = LocalDate.of(2026, 1, 15),
            endDate = null,
            lastGeneratedDate = null,
            isActive = true,
        )
        val entity = mapper.toEntity(recurring)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(recurring, roundTrip)
        assertEquals(7L, entity.sourceCardId)
        assertNull(entity.sourceWalletId)
        assertNull(entity.sourceStashId)
        assertEquals("MONTHLY", entity.frequency)
        assertEquals(1, entity.isActive)
    }

    @Test
    fun `wallet source round trip sets source_wallet_id to singleton`() {
        val recurring = RecurringExpense(
            id = 0L,
            amount = Money.of("30.00", Currency.USD),
            currency = "USD",
            categoryId = 2L,
            sourceType = SourceType.WALLET,
            sourceId = null,
            description = "Gym",
            frequency = Frequency.WEEKLY,
            startDate = LocalDate.of(2026, 2, 1),
            endDate = LocalDate.of(2026, 12, 31),
            lastGeneratedDate = LocalDate.of(2026, 2, 8),
            isActive = true,
        )
        val entity = mapper.toEntity(recurring)

        assertEquals(1L, entity.sourceWalletId)
        assertNull(entity.sourceCardId)
        assertNull(entity.sourceStashId)

        val roundTrip = mapper.toDomain(entity)
        assertEquals(recurring, roundTrip)
        assertEquals(SourceType.WALLET, roundTrip.sourceType)
        assertNull(roundTrip.sourceId)
    }

    @Test
    fun `stash source round trip with inactive flag`() {
        val recurring = RecurringExpense(
            id = 5L,
            amount = Money.of("100.00", Currency.MLC),
            currency = "MLC",
            categoryId = 3L,
            sourceType = SourceType.STASH,
            sourceId = 4L,
            description = "Savings target",
            frequency = Frequency.YEARLY,
            startDate = LocalDate.of(2026, 1, 1),
            endDate = null,
            lastGeneratedDate = null,
            isActive = false,
        )
        val entity = mapper.toEntity(recurring)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(recurring, roundTrip)
        assertEquals(4L, entity.sourceStashId)
        assertEquals(0, entity.isActive)
        assertEquals(false, roundTrip.isActive)
    }

    @Test
    fun `frequency maps to and from enum name`() {
        for (freq in Frequency.values()) {
            val recurring = RecurringExpense(
                id = 0L,
                amount = Money.of("10.00", Currency.CUP),
            currency = "CUP",
            categoryId = 1L,
                sourceType = SourceType.WALLET,
                sourceId = null,
                description = "test",
                frequency = freq,
                startDate = LocalDate.of(2026, 1, 1),
                endDate = null,
                lastGeneratedDate = null,
                isActive = true,
            )
            val entity = mapper.toEntity(recurring)
            val roundTrip = mapper.toDomain(entity)
            assertEquals(freq, roundTrip.frequency)
        }
    }

    @Test
    fun `amount decomposes into minor units and currency code`() {
        val recurring = RecurringExpense(
            id = 0L,
            amount = Money.of("42.50", Currency.USD),
            currency = "USD",
            categoryId = 1L,
            sourceType = SourceType.WALLET,
            sourceId = null,
            description = "test",
            frequency = Frequency.MONTHLY,
            startDate = LocalDate.of(2026, 1, 1),
            endDate = null,
            lastGeneratedDate = null,
            isActive = true,
        )
        val entity = mapper.toEntity(recurring)

        assertEquals(4250L, entity.amountMinor)
        assertEquals("USD", entity.amountCurrency)
    }

    @Test
    fun `dates map to epoch day and back`() {
        val recurring = RecurringExpense(
            id = 0L,
            amount = Money.of("10.00", Currency.CUP),
            currency = "CUP",
            categoryId = 1L,
            sourceType = SourceType.WALLET,
            sourceId = null,
            description = "test",
            frequency = Frequency.DAILY,
            startDate = LocalDate.of(2026, 3, 15),
            endDate = LocalDate.of(2026, 6, 20),
            lastGeneratedDate = LocalDate.of(2026, 3, 16),
            isActive = true,
        )
        val entity = mapper.toEntity(recurring)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(recurring, roundTrip)
        assertEquals(LocalDate.of(2026, 3, 15).toEpochDay(), entity.startDate)
        assertEquals(LocalDate.of(2026, 6, 20).toEpochDay(), entity.endDate)
        assertEquals(LocalDate.of(2026, 3, 16).toEpochDay(), entity.lastGeneratedDate)
    }
}
