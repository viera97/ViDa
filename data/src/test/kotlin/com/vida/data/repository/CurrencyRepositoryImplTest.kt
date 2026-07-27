package com.vida.data.repository

import app.cash.turbine.test
import com.vida.data.db.dao.CurrencyDao
import com.vida.data.db.entity.CurrencyEntity
import com.vida.data.mapper.CurrencyMapper
import com.vida.domain.model.CurrencyInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CurrencyRepositoryImplTest {
    private val dao = mockk<CurrencyDao>(relaxed = true)
    private val mapper = CurrencyMapper
    private lateinit var repository: CurrencyRepositoryImpl

    @Before
    fun setUp() {
        repository = CurrencyRepositoryImpl(dao, mapper)
    }

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        val entity = CurrencyEntity(id = 1L, name = "Dólar", code = "USD", isSystem = 1)
        coEvery { dao.observeAll() } returns flowOf(listOf(entity))

        repository.getAll().test {
            val currencies = awaitItem()
            assertEquals(1, currencies.size)
            assertEquals("Dólar", currencies[0].name)
            assertEquals("USD", currencies[0].code)
            assertEquals(true, currencies[0].isSystem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAll emits empty list when DAO is empty`() = runTest {
        coEvery { dao.observeAll() } returns flowOf(emptyList())

        repository.getAll().test {
            val currencies = awaitItem()
            assertEquals(0, currencies.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns mapped currency when found`() = runTest {
        val entity = CurrencyEntity(id = 1L, name = "Dólar", code = "USD", isSystem = 1)
        coEvery { dao.getById(1L) } returns entity

        val currency = repository.getById(1L)
        assertEquals("Dólar", currency!!.name)
        assertEquals(true, currency.isSystem)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(any()) } returns null

        assertNull(repository.getById(42L))
    }

    @Test
    fun `getByCode delegates to dao and returns mapped currency`() = runTest {
        val entity = CurrencyEntity(id = 1L, name = "Dólar", code = "USD", isSystem = 1)
        coEvery { dao.getByCode("USD") } returns entity

        val currency = repository.getByCode("USD")
        assertEquals("Dólar", currency!!.name)
        assertEquals(true, currency.isSystem)
        coVerify { dao.getByCode("USD") }
    }

    @Test
    fun `getByCode returns null when not found`() = runTest {
        coEvery { dao.getByCode(any()) } returns null

        assertNull(repository.getByCode("UNKNOWN"))
    }

    @Test
    fun `upsert delegates to dao and returns id`() = runTest {
        val currency = CurrencyInfo(id = 0L, name = "Bitcoin", code = "BTC", isSystem = false)
        val entity = mapper.toEntity(currency)
        coEvery { dao.upsert(entity) } returns 7L

        val id = repository.upsert(currency)
        assertEquals(7L, id)
        coVerify { dao.upsert(entity) }
    }

    @Test
    fun `upsert preserves isSystem flag when mapping to entity`() = runTest {
        val currency = CurrencyInfo(id = 0L, name = "USD", code = "USD", isSystem = true)
        val entity = mapper.toEntity(currency)
        coEvery { dao.upsert(entity) } returns 1L

        repository.upsert(currency)

        coVerify { dao.upsert(match { it.isSystem == 1 && it.code == "USD" }) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(1L)
        coVerify { dao.delete(1L) }
    }
}
