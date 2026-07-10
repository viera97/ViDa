package com.vida.domain.usecase.bank

import com.vida.domain.model.Bank
import com.vida.domain.repository.BankRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ListBanksTest {

    @Test
    fun `returns all banks from repository`() = runTest {
        val banks = listOf(
            Bank(name = "Bandec", color = 0xFF8E0509.toInt(), isSystem = true),
            Bank(name = "BPA", color = 0xFFBCD1DA.toInt(), isSystem = true),
        )
        val repo = mockk<BankRepository>(relaxed = true)
        every { repo.getAll() } returns flowOf(banks)

        val result = ListBanks(repo).invoke().first()

        assertEquals(banks, result)
    }
}
