package com.vida.domain.usecase.currency

import com.vida.domain.model.CurrencyInfo
import com.vida.domain.repository.CurrencyRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetCurrencyTest {

    @Test
    fun `returns currency when found`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)
        val currency = CurrencyInfo(id = 1L, name = "Dólar", code = "USD", isSystem = true)
        coEvery { repo.getById(1L) } returns currency

        val result = GetCurrency(repo).invoke(1L)

        assertEquals(currency, result)
    }

    @Test
    fun `returns null when not found`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)
        coEvery { repo.getById(any()) } returns null

        val result = GetCurrency(repo).invoke(99L)

        assertNull(result)
    }
}
