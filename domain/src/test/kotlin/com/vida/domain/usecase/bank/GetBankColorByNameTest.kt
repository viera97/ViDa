package com.vida.domain.usecase.bank

import com.vida.domain.model.Bank
import com.vida.domain.repository.BankRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetBankColorByNameTest {

    @Test
    fun `returns color when bank is found`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)
        val bank = Bank(name = "Bandec", color = 0xFF8E0509.toInt(), isSystem = true)
        coEvery { repo.getByName("Bandec") } returns bank

        val color = GetBankColorByName(repo).invoke("Bandec")

        assertEquals(0xFF8E0509.toInt(), color)
    }

    @Test
    fun `returns fallback color when bank is not found`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)
        coEvery { repo.getByName(any()) } returns null

        val color = GetBankColorByName(repo).invoke("Unknown")

        assertEquals(0xFF607D8B.toInt(), color)
    }
}
