package com.vida.data.repository

import app.cash.turbine.test
import com.vida.data.db.dao.BalanceDao
import com.vida.data.db.dao.StashDao
import com.vida.data.db.entity.CupTotalEntity
import com.vida.data.db.entity.StashEntity
import com.vida.data.mapper.StashMapper
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Stash
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class StashRepositoryImplTest {
    private val dao = mockk<StashDao>(relaxed = true)
    private val balanceDao = mockk<BalanceDao>(relaxed = true)
    private val mapper = StashMapper
    private lateinit var repository: StashRepositoryImpl

    @Before
    fun setUp() {
        repository = StashRepositoryImpl(dao, balanceDao, mapper)
    }

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        val entity = aStashEntity()
        coEvery { dao.observeAll() } returns flowOf(listOf(entity))

        repository.getAll().test {
            val stashes = awaitItem()
            assertEquals(1, stashes.size)
            assertEquals("Savings", stashes[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns mapped stash when found`() = runTest {
        val entity = aStashEntity()
        coEvery { dao.getById(1L) } returns entity

        val stash = repository.getById(1L)
        assertEquals("Savings", stash!!.name)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(any()) } returns null
        assertNull(repository.getById(42L))
    }

    @Test
    fun `upsert delegates to dao and returns id`() = runTest {
        val stash = aStash()
        val entity = mapper.toEntity(stash)
        coEvery { dao.upsert(entity) } returns 7L

        val id = repository.upsert(stash)
        assertEquals(7L, id)
        coVerify { dao.upsert(entity) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(1L)
        coVerify { dao.delete(1L) }
    }

    @Test
    fun `getBalance delegates to BalanceDao and converts minor units to Money in CUP`() = runTest {
        val asOf = Instant.ofEpochMilli(10_000L)
        every { balanceDao.getStashBalance(1L, asOf.toEpochMilli()) } returns
            flowOf(CupTotalEntity(20_000L))

        val balance = repository.getBalance(1L, asOf)

        // 20000 minor = 200.00 CUP
        assertEquals(Money.of("200.00", Currency.CUP), balance)
    }

    @Test
    fun `getBalance returns ZERO_CUP when BalanceDao emits null`() = runTest {
        every { balanceDao.getStashBalance(any(), any()) } returns flowOf(null)

        val balance = repository.getBalance(1L)

        assertEquals(Money.of("0.00", Currency.CUP), balance)
    }

    private fun aStashEntity() = StashEntity(
        name = "Savings",
        createdAt = Instant.ofEpochMilli(1000),
        updatedAt = Instant.ofEpochMilli(1000),
        currency = Currency.CUP,
    )

    private fun aStash() = Stash(
        name = "Savings",
        createdAt = Instant.ofEpochMilli(1000),
        updatedAt = Instant.ofEpochMilli(1000),
        currency = Currency.CUP,
    )
}
