package com.vida.data.repository

import app.cash.turbine.test
import com.vida.data.db.dao.RecurringExpenseDao
import com.vida.data.db.entity.RecurringExpenseEntity
import com.vida.data.mapper.RecurringExpenseMapper
import com.vida.domain.model.Currency
import com.vida.domain.model.Frequency
import com.vida.domain.model.Money
import com.vida.domain.model.RecurringExpense
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
import java.time.LocalDate

class RecurringExpenseRepositoryImplTest {
    private val dao = mockk<RecurringExpenseDao>(relaxed = true)
    private val mapper = RecurringExpenseMapper
    private lateinit var repository: RecurringExpenseRepositoryImpl

    @Before
    fun setUp() {
        repository = RecurringExpenseRepositoryImpl(dao, mapper)
    }

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        val entity = anEntity()
        coEvery { dao.observeAll() } returns flowOf(listOf(entity))

        repository.getAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Netflix", items[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns mapped recurring expense when found`() = runTest {
        coEvery { dao.getById(1L) } returns anEntity()

        val result = repository.getById(1L)
        assertEquals(Frequency.MONTHLY, result!!.frequency)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(any()) } returns null
        assertNull(repository.getById(42L))
    }

    @Test
    fun `getDue delegates to observeActive with epoch day`() = runTest {
        val asOf = LocalDate.of(2026, 3, 15)
        coEvery { dao.observeActive(any()) } returns flowOf(listOf(anEntity()))

        repository.getDue(asOf).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { dao.observeActive(asOf.toEpochDay()) }
    }

    @Test
    fun `upsert delegates to dao and returns id`() = runTest {
        val recurring = aRecurring()
        val entity = mapper.toEntity(recurring)
        coEvery { dao.upsert(entity) } returns 9L

        val id = repository.upsert(recurring)
        assertEquals(9L, id)
        coVerify { dao.upsert(entity) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(1L)
        coVerify { dao.delete(1L) }
    }

    private fun anEntity() = RecurringExpenseEntity(
        id = 10L,
        categoryId = 1L,
        amountMinor = 5000L,
        amountCurrency = "CUP",
        description = "Netflix",
        frequency = "MONTHLY",
        startDate = LocalDate.of(2026, 1, 1).toEpochDay(),
        endDate = null,
        lastGeneratedDate = null,
        isActive = 1,
        sourceWalletId = 1L,
        sourceCardId = null,
        sourceStashId = null,
    )

    private fun aRecurring() = RecurringExpense(
        id = 0L,
        amount = Money.of("50.00", Currency.CUP),
        currency = Currency.CUP,
        categoryId = 1L,
        sourceType = SourceType.WALLET,
        sourceId = null,
        description = "Netflix",
        frequency = Frequency.MONTHLY,
        startDate = LocalDate.of(2026, 1, 1),
        endDate = null,
        lastGeneratedDate = null,
        isActive = true,
    )
}