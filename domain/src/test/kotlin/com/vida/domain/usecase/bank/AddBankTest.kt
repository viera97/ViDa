package com.vida.domain.usecase.bank

import com.vida.domain.model.Bank
import com.vida.domain.repository.BankRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddBankTest {

    @Test
    fun `adds bank via repository and returns id`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)
        coEvery { repo.upsert(any<Bank>()) } returns 42L

        val bank = Bank(name = "Mi Banco", color = 0xFF123456.toInt())
        val id = AddBank(repo).invoke(bank)

        assertEquals(42L, id)
        coVerify(exactly = 1) { repo.upsert(bank) }
    }
}
