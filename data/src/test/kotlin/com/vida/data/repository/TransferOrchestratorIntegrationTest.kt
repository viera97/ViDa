package com.vida.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.CardEntity
import com.vida.data.db.entity.StashEntity
import com.vida.data.db.entity.WalletEntity
import com.vida.data.mapper.TransferMapper
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Transfer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransferOrchestratorIntegrationTest {
    private lateinit var database: AppDatabase
    private lateinit var orchestrator: TransferOrchestrator

    private var cardId: Long = 0L
    private var stashId: Long = 0L

    @Before
    fun setUp() = kotlinx.coroutines.runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

        orchestrator = TransferOrchestrator(
            database = database,
            transferDao = database.transferDao(),
            walletDao = database.walletDao(),
            cardDao = database.cardDao(),
            stashDao = database.stashDao(),
            transferMapper = TransferMapper,
        )

        database.walletDao().upsert(WalletEntity(id = 1L, currency = "CUP"))
        cardId = database.cardDao().upsert(
            CardEntity(
                maskedNumber = "123456******7890",
                bank = "POP",
                type = com.vida.domain.model.CardType.DEBIT,
                currency = "CUP",
                note = null,
                expirationDate = LocalDate.of(2028, 12, 31),
            ),
        )
        stashId = database.stashDao().upsert(
            StashEntity(
                name = "Emergency",
                createdAt = Instant.ofEpochMilli(0L),
                updatedAt = Instant.ofEpochMilli(0L),
                currency = Currency.CUP,
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `recordTransfer atomically inserts transfer after verifying sources`() = runTest {
        val transfer = Transfer(
            id = 0L,
            fromType = SourceType.WALLET,
            fromId = 1L,
            toType = SourceType.CARD,
            toId = cardId,
            amount = Money.of("200.00", Currency.CUP),
            dateTime = Instant.ofEpochMilli(1_000L),
            note = "test",
        )

        val id = orchestrator.recordTransfer(transfer)

        assertTrue("Transfer should get a positive row id", id > 0L)
        val persisted = database.transferDao().getById(id)
        assertNotNull("Transfer record must exist in transfers table", persisted)
        assertEquals(20000L, persisted!!.amountMinor)
    }

    @Test
    fun `recordTransfer wallet to stash succeeds`() = runTest {
        val transfer = Transfer(
            id = 0L,
            fromType = SourceType.WALLET,
            fromId = 1L,
            toType = SourceType.STASH,
            toId = stashId,
            amount = Money.of("100.00", Currency.CUP),
            dateTime = Instant.ofEpochMilli(2_000L),
            note = null,
        )

        val id = orchestrator.recordTransfer(transfer)
        val persisted = database.transferDao().getById(id)
        assertNotNull(persisted)
        assertEquals(stashId, persisted!!.destinationStashId)
    }

    @Test
    fun `recordTransfer card to stash succeeds`() = runTest {
        val transfer = Transfer(
            id = 0L,
            fromType = SourceType.CARD,
            fromId = cardId,
            toType = SourceType.STASH,
            toId = stashId,
            amount = Money.of("50.00", Currency.CUP),
            dateTime = Instant.ofEpochMilli(3_000L),
            note = "rebalance",
        )

        val id = orchestrator.recordTransfer(transfer)
        val persisted = database.transferDao().getById(id)
        assertNotNull(persisted)
        assertEquals(cardId, persisted!!.sourceCardId)
        assertEquals(stashId, persisted.destinationStashId)
    }

    @Test
    fun `recordTransfer rolls back when source does not exist`() = runTest {
        val transfer = Transfer(
            id = 0L,
            fromType = SourceType.CARD,
            fromId = 999L, // non-existent card
            toType = SourceType.WALLET,
            toId = 1L,
            amount = Money.of("100.00", Currency.CUP),
            dateTime = Instant.ofEpochMilli(4_000L),
            note = null,
        )

        var threw = false
        try {
            orchestrator.recordTransfer(transfer)
        } catch (_: IllegalStateException) {
            threw = true
        }
        assertTrue("Expected IllegalStateException for non-existent source", threw)

        // Verify no transfer was persisted (rollback)
        val all = database.transferDao().observeAll().first()
        assertTrue("No transfer record should exist after rollback", all.isEmpty())
    }

    @Test
    fun `recordTransfer rolls back when destination does not exist`() = runTest {
        val transfer = Transfer(
            id = 0L,
            fromType = SourceType.WALLET,
            fromId = 1L,
            toType = SourceType.STASH,
            toId = 999L, // non-existent stash
            amount = Money.of("100.00", Currency.CUP),
            dateTime = Instant.ofEpochMilli(5_000L),
            note = null,
        )

        var threw = false
        try {
            orchestrator.recordTransfer(transfer)
        } catch (_: IllegalStateException) {
            threw = true
        }
        assertTrue("Expected IllegalStateException for non-existent destination", threw)

        val all = database.transferDao().observeAll().first()
        assertTrue("No transfer record should exist after rollback", all.isEmpty())
    }
}