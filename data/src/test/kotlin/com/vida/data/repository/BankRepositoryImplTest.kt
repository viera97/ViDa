package com.vida.data.repository

import app.cash.turbine.test
import com.vida.data.db.dao.BankDao
import com.vida.data.db.entity.BankEntity
import com.vida.data.mapper.BankMapper
import com.vida.domain.model.Bank
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BankRepositoryImplTest {
    private val dao = mockk<BankDao>(relaxed = true)
    private val mapper = BankMapper
    private lateinit var repository: BankRepositoryImpl

    @Before
    fun setUp() {
        repository = BankRepositoryImpl(dao, mapper)
    }

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        val entity = BankEntity(id = 1L, name = "Bandec", color = 0xFF8E0509.toInt(), isSystem = 1)
        coEvery { dao.observeAll() } returns flowOf(listOf(entity))

        repository.getAll().test {
            val banks = awaitItem()
            assertEquals(1, banks.size)
            assertEquals("Bandec", banks[0].name)
            assertEquals(true, banks[0].isSystem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns mapped bank when found`() = runTest {
        val entity = BankEntity(id = 1L, name = "Bandec", color = 0xFF8E0509.toInt(), isSystem = 1)
        coEvery { dao.getById(1L) } returns entity

        val bank = repository.getById(1L)
        assertEquals("Bandec", bank!!.name)
        assertEquals(true, bank.isSystem)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(any()) } returns null

        assertNull(repository.getById(42L))
    }

    @Test
    fun `getByName delegates to dao and returns mapped bank`() = runTest {
        val entity = BankEntity(id = 1L, name = "Bandec", color = 0xFF8E0509.toInt(), isSystem = 1)
        coEvery { dao.getByName("Bandec") } returns entity

        val bank = repository.getByName("Bandec")
        assertEquals("Bandec", bank!!.name)
        coVerify { dao.getByName("Bandec") }
    }

    @Test
    fun `getByName returns null when not found`() = runTest {
        coEvery { dao.getByName(any()) } returns null

        assertNull(repository.getByName("Unknown"))
    }

    @Test
    fun `upsert delegates to dao and returns id`() = runTest {
        val bank = Bank(id = 0L, name = "Bandec", color = 0xFF8E0509.toInt(), isSystem = true)
        val entity = mapper.toEntity(bank)
        coEvery { dao.upsert(entity) } returns 7L

        val id = repository.upsert(bank)
        assertEquals(7L, id)
        coVerify { dao.upsert(entity) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(1L)
        coVerify { dao.delete(1L) }
    }
}
