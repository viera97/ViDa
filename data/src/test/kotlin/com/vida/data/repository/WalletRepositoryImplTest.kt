package com.vida.data.repository

import com.vida.data.db.dao.WalletDao
import com.vida.data.db.entity.WalletEntity
import com.vida.data.mapper.WalletMapper
import com.vida.domain.model.Currency
import com.vida.domain.model.Wallet
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class WalletRepositoryImplTest {
    private val dao = mockk<WalletDao>(relaxed = true)
    private val mapper = WalletMapper
    private lateinit var repository: WalletRepositoryImpl

    @Before
    fun setUp() {
        repository = WalletRepositoryImpl(dao, mapper)
    }

    @Test
    fun `get returns mapped wallet`() = runTest {
        val entity = WalletEntity(currency = Currency.CUP)
        coEvery { dao.get() } returns entity

        val wallet = repository.get()
        assertEquals(Currency.CUP, wallet.currency)
    }

    @Test
    fun `get throws when wallet not found`() = runTest {
        coEvery { dao.get() } returns null

        try {
            repository.get()
            assert(false) { "Expected NoSuchElementException" }
        } catch (e: NoSuchElementException) {
            // Expected
        }
    }

    @Test
    fun `upsert delegates to dao`() = runTest {
        val wallet = Wallet(currency = Currency.USD)
        repository.upsert(wallet)
        coVerify { dao.upsert(WalletEntity(currency = Currency.USD)) }
    }

    @Test
    fun `upsert with non-1 id throws`() = runTest {
        // Use mock to bypass domain constructor's id=1 enforcement
        val wallet = mockk<Wallet> {
            every { id } returns 2L
            every { currency } returns Currency.CUP
        }

        try {
            repository.upsert(wallet)
            assert(false) { "Expected IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `getBalance throws NotImplementedError`() = runTest {
        try {
            repository.getBalance()
            assert(false) { "Expected NotImplementedError" }
        } catch (_: NotImplementedError) {
            // Expected
        }
    }
}
