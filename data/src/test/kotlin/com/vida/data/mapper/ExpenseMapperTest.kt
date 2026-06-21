package com.vida.data.mapper

import com.vida.data.db.entity.ExpenseEntity
import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ExpenseMapperTest {
    private val mapper = ExpenseMapper

    @Test
    fun `card source round trip preserves all fields`() {
        val expense = Expense(
            id = 10L,
            categoryId = 1L,
            amount = Money.of("42.50", Currency.USD),
            realAmount = null,
            description = "Lunch",
            dateTime = Instant.ofEpochMilli(5_000_000L),
            sourceType = SourceType.CARD,
            sourceId = 7L,
            note = "team lunch",
        )
        val entity = mapper.toEntity(expense)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(expense, roundTrip)
        assertEquals(7L, entity.sourceCardId)
        assertNull(entity.sourceWalletId)
        assertNull(entity.sourceStashId)
    }

    @Test
    fun `stash source round trip`() {
        val expense = Expense(
            id = 11L,
            categoryId = 1L,
            amount = Money.of("10.00", Currency.CUP),
            description = "Coffee",
            dateTime = Instant.ofEpochMilli(6_000_000L),
            sourceType = SourceType.STASH,
            sourceId = 3L,
        )
        val entity = mapper.toEntity(expense)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(expense, roundTrip)
        assertEquals(3L, entity.sourceStashId)
        assertNull(entity.sourceCardId)
        assertNull(entity.sourceWalletId)
    }

    @Test
    fun `wallet source round trip sets source_wallet_id to singleton`() {
        val expense = Expense(
            id = 12L,
            categoryId = 1L,
            amount = Money.of("100.00", Currency.CUP),
            description = "Cash spend",
            dateTime = Instant.ofEpochMilli(7_000_000L),
            sourceType = SourceType.WALLET,
            sourceId = null,
        )
        val entity = mapper.toEntity(expense)

        // Wallet is addressed via the singleton id=1; the other two source columns stay null.
        assertEquals(1L, entity.sourceWalletId)
        assertNull(entity.sourceCardId)
        assertNull(entity.sourceStashId)

        val roundTrip = mapper.toDomain(entity)
        assertEquals(expense, roundTrip)
        assertEquals(SourceType.WALLET, roundTrip.sourceType)
        assertNull(roundTrip.sourceId)
    }

    @Test
    fun `round trip preserves realAmount and note`() {
        val expense = Expense(
            id = 13L,
            categoryId = 2L,
            amount = Money.of("20.00", Currency.MLC),
            realAmount = Money.of("20.50", Currency.MLC),
            description = "Tip included",
            dateTime = Instant.ofEpochMilli(8_000_000L),
            sourceType = SourceType.CARD,
            sourceId = 9L,
            note = "rounded up",
        )
        val entity = mapper.toEntity(expense)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(expense, roundTrip)
        assertEquals(2050L, entity.realAmountMinor)
        assertEquals("MLC", entity.realAmountCurrency)
    }

    @Test
    fun `amount decomposes into minor units and currency code`() {
        val expense = Expense(
            id = 0L,
            categoryId = 1L,
            amount = Money.of("12.34", Currency.CUP),
            description = "x",
            dateTime = Instant.ofEpochMilli(0L),
            sourceType = SourceType.WALLET,
            sourceId = null,
        )
        val entity = mapper.toEntity(expense)

        assertEquals(1234L, entity.amountMinor)
        assertEquals("CUP", entity.amountCurrency)
        assertNull(entity.realAmountMinor)
        assertNull(entity.realAmountCurrency)
    }

    @Test
    fun `all currencies round trip via amount decomposition`() {
        for (currency in Currency.values()) {
            val expense = Expense(
                id = 0L,
                categoryId = 1L,
                amount = Money.of("99.99", currency),
                description = "c",
                dateTime = Instant.ofEpochMilli(1_000L),
                sourceType = SourceType.WALLET,
                sourceId = null,
            )
            val entity = mapper.toEntity(expense)
            val roundTrip = mapper.toDomain(entity)
            assertEquals(expense, roundTrip)
        }
    }
}
