package com.vida.data.repository

import com.vida.data.db.dao.BalanceDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.db.entity.CupTotalEntity
import com.vida.data.db.entity.WalletEntity
import com.vida.data.mapper.WalletMapper
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Wallet
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.time.Instant

class WalletRepositoryImplTest {
    private val dao = mockk<WalletDao>(relaxed = true)
    private val balanceDao = mockk<BalanceDao>(relaxed = true)
    private val mapper = WalletMapper
    private lateinit var repository: WalletRepositoryImpl

    @Before
    fun setUp() {
        repository = WalletRepositoryImpl(dao, balanceDao, mapper)
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
    fun `getBalance delegates to BalanceDao and converts minor units to Money in CUP`() = runTest {
        val asOf = Instant.ofEpochMilli(10_000L)
        every { balanceDao.getWalletBalance(asOf.toEpochMilli()) } returns
            flowOf(CupTotalEntity(50_000L))

        val balance = repository.getBalance(asOf)

        // 50000 minor = 500.00 CUP
        assertEquals(Money.of("500.00", Currency.CUP), balance)
    }

    @Test
    fun `getBalance returns ZERO_CUP when BalanceDao emits null`() = runTest {
        every { balanceDao.getWalletBalance(any()) } returns flowOf(null)

        val balance = repository.getBalance()

        assertEquals(Money.of("0.00", Currency.CUP), balance)
    }
}
