package com.vida.domain.usecase.currency

import com.vida.domain.model.CurrencyInfo
import com.vida.domain.repository.CurrencyRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ListCurrenciesTest {

    @Test
    fun `returns all currencies from repository`() = runTest {
        val currencies = listOf(
            CurrencyInfo(name = "Peso cubano", code = "CUP", isSystem = true),
            CurrencyInfo(name = "Dólar", code = "USD", isSystem = true),
        )
        val repo = mockk<CurrencyRepository>(relaxed = true)
        every { repo.getAll() } returns flowOf(currencies)

        val result = ListCurrencies(repo).invoke().first()

        assertEquals(currencies, result)
    }

    @Test
    fun `returns empty list when repository has no currencies`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)
        every { repo.getAll() } returns flowOf(emptyList())

        val result = ListCurrencies(repo).invoke().first()

        assertEquals(emptyList<CurrencyInfo>(), result)
    }
}
