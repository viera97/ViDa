package com.vida.domain.usecase.bank

import com.vida.domain.model.Bank
import com.vida.domain.repository.BankRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteBankTest {

    @Test
    fun `deletes bank via repository`() = runTest {
        val repo = mockk<BankRepository>(relaxed = true)

        DeleteBank(repo).invoke(1L)

        coVerify(exactly = 1) { repo.delete(1L) }
    }
}
