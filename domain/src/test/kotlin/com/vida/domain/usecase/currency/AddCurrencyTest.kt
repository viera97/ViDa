package com.vida.domain.usecase.currency

import com.vida.domain.model.CurrencyInfo
import com.vida.domain.repository.CurrencyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddCurrencyTest {

    @Test
    fun `adds currency via repository and returns id`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)
        coEvery { repo.upsert(any<CurrencyInfo>()) } returns 42L

        val currency = CurrencyInfo(name = "Bitcoin", code = "BTC")
        val id = AddCurrency(repo).invoke(currency)

        assertEquals(42L, id)
        coVerify(exactly = 1) { repo.upsert(currency) }
    }
}
