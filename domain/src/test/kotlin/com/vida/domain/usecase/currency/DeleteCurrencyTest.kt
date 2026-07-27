package com.vida.domain.usecase.currency

import com.vida.domain.repository.CurrencyRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteCurrencyTest {

    @Test
    fun `deletes currency via repository`() = runTest {
        val repo = mockk<CurrencyRepository>(relaxed = true)

        DeleteCurrency(repo).invoke(1L)

        coVerify(exactly = 1) { repo.delete(1L) }
    }
}
