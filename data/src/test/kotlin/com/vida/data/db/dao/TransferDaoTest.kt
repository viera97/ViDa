package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.CardEntity
import com.vida.data.db.entity.StashEntity
import com.vida.data.db.entity.TransferEntity
import com.vida.data.db.entity.WalletEntity
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransferDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: TransferDao
    private var cardId: Long = 0L
    private var stashId: Long = 0L

    @Before
    fun setUp() = kotlinx.coroutines.runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.transferDao()
        cardId = database.cardDao().upsert(
            CardEntity(
                maskedNumber = "123456******7890",
                bank = "POP",
                type = CardType.DEBIT,
                currency = "USD",
                note = null,
                expirationDate = LocalDate.of(2028, 12, 31),
            ),
        )
        stashId = database.stashDao().upsert(
            StashEntity(
                name = "Emergency",
                createdAt = java.time.Instant.ofEpochMilli(0L),
                updatedAt = java.time.Instant.ofEpochMilli(0L),
                currency = Currency.USD,
            ),
        )
        database.walletDao().upsert(WalletEntity(id = 1L, currency = "CUP"))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert then observeAll emits entity`() = runTest {
        dao.upsert(aTransfer(sourceCardId = cardId, destinationStashId = stashId))
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(10000L, items[0].amountMinor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns entity after upsert`() = runTest {
        val id = dao.upsert(aTransfer(sourceCardId = cardId, destinationStashId = stashId))
        val result = dao.getById(id)
        assertEquals("CUP", result!!.amountCurrency)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        assertNull(dao.getById(999L))
    }

    @Test
    fun `delete removes row`() = runTest {
        val id = dao.upsert(aTransfer(sourceCardId = cardId, destinationStashId = stashId))
        dao.delete(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun `observeBySource filters by card source`() = runTest {
        dao.upsert(aTransfer(sourceCardId = cardId, destinationStashId = stashId, dateTime = 100L))
        dao.upsert(aTransfer(sourceStashId = stashId, destinationCardId = cardId, dateTime = 100L))

        val cardItems = dao.observeBySource("CARD", cardId, 1_000L).first()
        assertEquals(1, cardItems.size)
        assertEquals(cardId, cardItems[0].sourceCardId)

        val stashItems = dao.observeBySource("STASH", stashId, 1_000L).first()
        assertEquals(1, stashItems.size)
        assertEquals(stashId, stashItems[0].sourceStashId)
    }

    @Test
    fun `observeBySource for wallet returns wallet-sourced transfers only`() = runTest {
        dao.upsert(aTransfer(sourceWalletId = 1L, destinationCardId = cardId, dateTime = 100L))
        dao.upsert(aTransfer(sourceCardId = cardId, destinationStashId = stashId, dateTime = 100L))

        val walletItems = dao.observeBySource("WALLET", null, 1_000L).first()
        assertEquals(1, walletItems.size)
        assertEquals(1L, walletItems[0].sourceWalletId)
        assertNull(walletItems[0].sourceCardId)
    }

    @Test
    fun `observeByDestination filters by destination`() = runTest {
        dao.upsert(aTransfer(sourceCardId = cardId, destinationStashId = stashId, dateTime = 100L))
        dao.upsert(aTransfer(sourceStashId = stashId, destinationCardId = cardId, dateTime = 100L))

        val stashDest = dao.observeByDestination("STASH", stashId, 1_000L).first()
        assertEquals(1, stashDest.size)
        assertEquals(stashId, stashDest[0].destinationStashId)

        val cardDest = dao.observeByDestination("CARD", cardId, 1_000L).first()
        assertEquals(1, cardDest.size)
        assertEquals(cardId, cardDest[0].destinationCardId)
    }

    @Test
    fun `observeByParticipant returns transfers from both sides`() = runTest {
        dao.upsert(aTransfer(sourceCardId = cardId, destinationStashId = stashId, dateTime = 100L))
        dao.upsert(aTransfer(sourceWalletId = 1L, destinationCardId = cardId, dateTime = 100L))

        val cardParticipant = dao.observeByParticipant("CARD", cardId, 1_000L).first()
        assertEquals(2, cardParticipant.size)
    }

    @Test
    fun `observeBySource respects asOf cutoff`() = runTest {
        dao.upsert(aTransfer(sourceCardId = cardId, destinationStashId = stashId, dateTime = 50L))
        dao.upsert(aTransfer(sourceCardId = cardId, destinationStashId = stashId, dateTime = 200L))

        val items = dao.observeBySource("CARD", cardId, 100L).first()
        assertEquals(1, items.size)
        assertEquals(50L, items[0].dateTime)
    }

    @Test
    fun `empty table returns empty list`() = runTest {
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(0, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun aTransfer(
        sourceWalletId: Long? = null,
        sourceCardId: Long? = null,
        sourceStashId: Long? = null,
        destinationWalletId: Long? = null,
        destinationCardId: Long? = null,
        destinationStashId: Long? = null,
        dateTime: Long = 1_000L,
    ) = TransferEntity(
        amountMinor = 10000L,
        amountCurrency = "CUP",
        dateTime = dateTime,
        note = "test transfer",
        sourceWalletId = sourceWalletId,
        sourceCardId = sourceCardId,
        sourceStashId = sourceStashId,
        destinationWalletId = destinationWalletId,
        destinationCardId = destinationCardId,
        destinationStashId = destinationStashId,
    )
}
