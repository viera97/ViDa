package com.vida.domain.usecase.currency

import com.vida.domain.model.CurrencyInfo
import com.vida.domain.repository.CurrencyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateCurrencyTest {

    @Test
    fun `updates existing currency via repository`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)
        val existing = CurrencyInfo(id = 1L, name = "Bitcoin", code = "BTC", isSystem = false)
        coEvery { repo.getById(1L) } returns existing
        coEvery { repo.upsert(any<CurrencyInfo>()) } returns 1L

        val updated = existing.copy(name = "Bitcoin Cash", code = "BCH")
        val id = UpdateCurrency(repo).invoke(updated)

        assertEquals(1L, id)
        coVerify(exactly = 1) { repo.upsert(updated) }
    }

    @Test
    fun `throws when currency does not exist`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)
        coEvery { repo.getById(any()) } returns null

        val currency = CurrencyInfo(id = 99L, name = "Ghost", code = "GHO")
        var threw = false
        try {
            UpdateCurrency(repo).invoke(currency)
        } catch (_: NoSuchElementException) {
            threw = true
        }
        assertEquals(true, threw)
        coVerify(inverse = true) { repo.upsert(any<CurrencyInfo>()) }
    }

    @Test
    fun `throws when currency id is zero`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)
        val currency = CurrencyInfo(name = "New", code = "NEW")
        var threw = false
        try {
            UpdateCurrency(repo).invoke(currency)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertEquals(true, threw)
        coVerify(inverse = true) { repo.getById(any()) }
        coVerify(inverse = true) { repo.upsert(any<CurrencyInfo>()) }
    }
}
