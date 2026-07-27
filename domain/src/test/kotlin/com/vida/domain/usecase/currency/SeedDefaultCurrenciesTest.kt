package com.vida.domain.usecase.currency

import com.vida.domain.model.CurrencyInfo
import com.vida.domain.model.DefaultCurrencies
import com.vida.domain.repository.CurrencyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SeedDefaultCurrenciesTest {

    @Test
    fun `seeds all four default currencies on empty repo`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)
        coEvery { repo.getAll() } returns flowOf(emptyList())
        coEvery { repo.upsert(any<CurrencyInfo>()) } returns 0L

        SeedDefaultCurrencies(repo).invoke()

        coVerify(exactly = 4) { repo.upsert(any<CurrencyInfo>()) }
    }

    @Test
    fun `is a no-op when currencies already exist`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)
        val existing = CurrencyInfo(id = 1L, name = "Dólar", code = "USD", isSystem = true)
        coEvery { repo.getAll() } returns flowOf(listOf(existing))

        SeedDefaultCurrencies(repo).invoke()

        coVerify(exactly = 0) { repo.upsert(any<CurrencyInfo>()) }
    }

    @Test
    fun `seeds currencies matching DefaultCurrencies ALL`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)
        coEvery { repo.getAll() } returns flowOf(emptyList())

        SeedDefaultCurrencies(repo).invoke()

        DefaultCurrencies.ALL.forEach { currency ->
            coVerify { repo.upsert(match { it.code == currency.code }) }
        }
    }

    @Test
    fun `seeded currencies are flagged as system`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)
        coEvery { repo.getAll() } returns flowOf(emptyList())

        SeedDefaultCurrencies(repo).invoke()

        DefaultCurrencies.ALL.forEach { currency ->
            coVerify { repo.upsert(match { it.code == currency.code && it.isSystem }) }
        }
    }
}
