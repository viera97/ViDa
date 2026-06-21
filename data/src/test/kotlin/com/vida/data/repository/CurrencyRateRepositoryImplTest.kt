package com.vida.data.repository

import app.cash.turbine.test
import com.vida.data.db.dao.CurrencyRateDao
import com.vida.data.db.entity.CurrencyRateEntity
import com.vida.data.mapper.CurrencyRateMapper
import com.vida.domain.model.Currency
import com.vida.domain.model.CurrencyRate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class CurrencyRateRepositoryImplTest {
    private val dao = mockk<CurrencyRateDao>(relaxed = true)
    private val mapper = CurrencyRateMapper
    private lateinit var repository: CurrencyRateRepositoryImpl

    @Before
    fun setUp() {
        repository = CurrencyRateRepositoryImpl(dao, mapper)
    }

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        val entity = anEntity()
        coEvery { dao.observeAll() } returns flowOf(listOf(entity))

        repository.getAll().test {
            val rates = awaitItem()
            assertEquals(1, rates.size)
            assertEquals(Currency.USD, rates[0].fromCurrency)
            assertEquals(Currency.CUP, rates[0].toCurrency)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getRate delegates to dao with currency codes and epoch millis`() = runTest {
        coEvery { dao.getRate(any(), any(), any()) } returns anEntity()

        val rate = repository.getRate(Currency.USD, Currency.CUP, Instant.ofEpochMilli(1_000L))
        assertEquals(Currency.USD, rate!!.fromCurrency)
        coVerify { dao.getRate("USD", "CUP", 1_000L) }
    }

    @Test
    fun `getRate returns null when dao returns null`() = runTest {
        coEvery { dao.getRate(any(), any(), any()) } returns null

        assertNull(repository.getRate(Currency.USD, Currency.MLC, Instant.ofEpochMilli(500L)))
    }

    @Test
    fun `getRateHistory delegates to dao with currency codes`() = runTest {
        coEvery { dao.observeRateHistory(any(), any()) } returns flowOf(listOf(anEntity()))

        repository.getRateHistory(Currency.USD, Currency.CUP).test {
            val rates = awaitItem()
            assertEquals(1, rates.size)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { dao.observeRateHistory("USD", "CUP") }
    }

    @Test
    fun `upsert delegates to dao and returns id`() = runTest {
        val rate = aRate()
        val entity = mapper.toEntity(rate)
        coEvery { dao.upsert(entity) } returns 3L

        val id = repository.upsert(rate)
        assertEquals(3L, id)
        coVerify { dao.upsert(entity) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(1L)
        coVerify { dao.delete(1L) }
    }

    private fun anEntity() = CurrencyRateEntity(
        id = 1L,
        fromCurrency = Currency.USD,
        toCurrency = Currency.CUP,
        rate = 24.5,
        effectiveDate = 1_000L,
    )

    private fun aRate() = CurrencyRate(
        id = 0L,
        fromCurrency = Currency.USD,
        toCurrency = Currency.CUP,
        rate = BigDecimal("24.50"),
        updatedAt = Instant.ofEpochMilli(1_000L),
    )
}
