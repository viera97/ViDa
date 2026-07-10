package com.vida.domain.usecase.bank

import com.vida.domain.model.Bank
import com.vida.domain.repository.BankRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateBankTest {

    @Test
    fun `updates existing bank via repository`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)
        val existing = Bank(id = 1L, name = "Bandec", color = 0xFF8E0509.toInt(), isSystem = true)
        coEvery { repo.getById(1L) } returns existing
        coEvery { repo.upsert(any<Bank>()) } returns 1L

        val updated = existing.copy(name = "Banco Bandec")
        val id = UpdateBank(repo).invoke(updated)

        assertEquals(1L, id)
        coVerify(exactly = 1) { repo.upsert(updated) }
    }

    @Test
    fun `throws when bank does not exist`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)
        coEvery { repo.getById(any()) } returns null

        val bank = Bank(id = 99L, name = "Ghost", color = 0)
        try {
            UpdateBank(repo).invoke(bank)
        } catch (_: NoSuchElementException) {
            return@runTest
        }
    }

    @Test
    fun `throws when bank id is zero`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)
        val bank = Bank(name = "New", color = 0)
        try {
            UpdateBank(repo).invoke(bank)
        } catch (_: IllegalArgumentException) {
            return@runTest
        }
    }
}
