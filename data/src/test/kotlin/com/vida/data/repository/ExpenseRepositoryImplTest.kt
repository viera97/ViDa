package com.vida.data.repository

import app.cash.turbine.test
import com.vida.data.db.dao.ExpenseDao
import com.vida.data.db.entity.ExpenseEntity
import com.vida.data.mapper.ExpenseMapper
import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ExpenseRepositoryImplTest {
    private val dao = mockk<ExpenseDao>(relaxed = true)
    private val mapper = ExpenseMapper
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        repository = ExpenseRepositoryImpl(dao, mapper)
    }

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        val entity = anEntity()
        coEvery { dao.observeAll() } returns flowOf(listOf(entity))

        repository.getAll().test {
            val expenses = awaitItem()
            assertEquals(1, expenses.size)
            assertEquals("Lunch", expenses[0].description)
            assertEquals(SourceType.CARD, expenses[0].sourceType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns mapped expense when found`() = runTest {
        coEvery { dao.getById(1L) } returns anEntity()

        val expense = repository.getById(1L)
        assertEquals("Lunch", expense!!.description)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(any()) } returns null
        assertNull(repository.getById(42L))
    }

    @Test
    fun `getBySource delegates to dao with source type name and epoch millis`() = runTest {
        coEvery { dao.observeBySource(any(), any(), any()) } returns flowOf(listOf(anEntity()))

        repository.getBySource(SourceType.CARD, 5L, Instant.ofEpochMilli(1_000L)).test {
            val expenses = awaitItem()
            assertEquals(1, expenses.size)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { dao.observeBySource("CARD", 5L, 1_000L) }
    }

    @Test
    fun `getByCategory delegates to dao with epoch millis`() = runTest {
        coEvery { dao.observeByCategory(any(), any()) } returns flowOf(listOf(anEntity()))

        repository.getByCategory(3L, Instant.ofEpochMilli(9_000L)).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { dao.observeByCategory(3L, 9_000L) }
    }

    @Test
    fun `getByDateRange delegates to dao with epoch millis`() = runTest {
        coEvery { dao.observeByDateRange(any(), any()) } returns flowOf(listOf(anEntity()))

        repository.getByDateRange(Instant.ofEpochMilli(100L), Instant.ofEpochMilli(200L)).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { dao.observeByDateRange(100L, 200L) }
    }

    @Test
    fun `upsert delegates to dao and returns id`() = runTest {
        val expense = anExpense()
        val entity = mapper.toEntity(expense)
        coEvery { dao.upsert(entity) } returns 11L

        val id = repository.upsert(expense)
        assertEquals(11L, id)
        coVerify { dao.upsert(entity) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(1L)
        coVerify { dao.delete(1L) }
    }

    private fun anEntity() = ExpenseEntity(
        id = 1L,
        categoryId = 2L,
        amountMinor = 1234L,
        amountCurrency = "USD",
        realAmountMinor = null,
        realAmountCurrency = null,
        description = "Lunch",
        dateTime = 1_000L,
        note = null,
        sourceWalletId = null,
        sourceCardId = 5L,
        sourceStashId = null,
    )

    private fun anExpense() = Expense(
        id = 0L,
        categoryId = 2L,
        amount = Money.of("12.34", Currency.USD),
        description = "Lunch",
        dateTime = Instant.ofEpochMilli(1_000L),
        sourceType = SourceType.CARD,
        sourceId = 5L,
    )
}
