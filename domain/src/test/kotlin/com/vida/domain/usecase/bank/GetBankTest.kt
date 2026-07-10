package com.vida.domain.usecase.bank

import com.vida.domain.model.Bank
import com.vida.domain.repository.BankRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetBankTest {

    @Test
    fun `returns bank when found`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)
        val bank = Bank(id = 1L, name = "Bandec", color = 0xFF8E0509.toInt(), isSystem = true)
        coEvery { repo.getById(1L) } returns bank

        val result = GetBank(repo).invoke(1L)

        assertEquals(bank, result)
    }

    @Test
    fun `returns null when not found`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)
        coEvery { repo.getById(any()) } returns null

        val result = GetBank(repo).invoke(99L)

        assertNull(result)
    }
}
