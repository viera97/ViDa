package com.vida.data.repository

import app.cash.turbine.test
import com.vida.data.db.dao.TransferDao
import com.vida.data.db.entity.TransferEntity
import com.vida.data.mapper.TransferMapper
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Transfer
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class TransferRepositoryImplTest {
    private val dao = mockk<TransferDao>(relaxed = true)
    private val mapper = TransferMapper
    private val orchestrator = mockk<TransferOrchestrator>(relaxed = true)
    private lateinit var repository: TransferRepositoryImpl

    @Before
    fun setUp() {
        repository = TransferRepositoryImpl(dao, mapper, orchestrator)
    }

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        val entity = anEntity()
        coEvery { dao.observeAll() } returns flowOf(listOf(entity))

        repository.getAll().test {
            val transfers = awaitItem()
            assertEquals(1, transfers.size)
            assertEquals(SourceType.CARD, transfers[0].fromType)
            assertEquals(5L, transfers[0].fromId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns mapped transfer when found`() = runTest {
        coEvery { dao.getById(1L) } returns anEntity()

        val transfer = repository.getById(1L)
        assertEquals(SourceType.CARD, transfer!!.fromType)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(any()) } returns null
        assertNull(repository.getById(42L))
    }

    @Test
    fun `getBySource delegates to observeByParticipant`() = runTest {
        coEvery { dao.observeByParticipant(any(), any(), any()) } returns flowOf(listOf(anEntity()))

        repository.getBySource(SourceType.CARD, 5L, Instant.ofEpochMilli(1_000L)).test {
            val transfers = awaitItem()
            assertEquals(1, transfers.size)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { dao.observeByParticipant("CARD", 5L, 1_000L) }
    }

    @Test
    fun `upsert delegates to orchestrator recordTransfer`() = runTest {
        val transfer = aTransfer()
        coEvery { orchestrator.recordTransfer(transfer) } returns 42L

        val id = repository.upsert(transfer)
        assertEquals(42L, id)
        coVerify { orchestrator.recordTransfer(transfer) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(1L)
        coVerify { dao.delete(1L) }
    }

    private fun anEntity() = TransferEntity(
        id = 10L,
        amountMinor = 10000L,
        amountCurrency = "CUP",
        dateTime = 5_000L,
        note = "test",
        sourceWalletId = null,
        sourceCardId = 5L,
        sourceStashId = null,
        destinationWalletId = 1L,
        destinationCardId = null,
        destinationStashId = null,
    )

    private fun aTransfer() = Transfer(
        id = 0L,
        fromType = SourceType.CARD,
        fromId = 5L,
        toType = SourceType.WALLET,
        toId = null,
        amount = Money.of("100.00", Currency.CUP),
        dateTime = Instant.ofEpochMilli(5_000L),
        note = "test",
    )
}