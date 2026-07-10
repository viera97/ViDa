package com.vida.domain.usecase.bank

import com.vida.domain.model.Bank
import com.vida.domain.model.DefaultBanks
import com.vida.domain.repository.BankRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SeedDefaultBanksTest {

    @Test
    fun `seeds all three default banks on empty repo`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)
        coEvery { repo.getAll() } returns flowOf(emptyList())
        coEvery { repo.upsert(any<Bank>()) } returns 0L

        SeedDefaultBanks(repo).invoke()

        coVerify(exactly = 3) { repo.upsert(any<Bank>()) }
    }

    @Test
    fun `is a no-op when banks already exist`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)
        val existing = Bank(id = 1L, name = "Bandec", color = 0xFF8E0509.toInt(), isSystem = true)
        coEvery { repo.getAll() } returns flowOf(listOf(existing))

        SeedDefaultBanks(repo).invoke()

        coVerify(exactly = 0) { repo.upsert(any<Bank>()) }
    }

    @Test
    fun `seeds banks matching DefaultBanks ALL`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)
        coEvery { repo.getAll() } returns flowOf(emptyList())

        SeedDefaultBanks(repo).invoke()

        DefaultBanks.ALL.forEach { bank ->
            coVerify { repo.upsert(match { it.name == bank.name }) }
        }
    }
}
