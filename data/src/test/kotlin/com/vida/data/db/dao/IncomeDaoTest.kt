package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.CardEntity
import com.vida.data.db.entity.IncomeEntity
import com.vida.data.db.entity.StashEntity
import com.vida.data.db.entity.WalletEntity
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IncomeDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: IncomeDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.incomeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Seeds a wallet, a card and a stash so the income rows have something to
     * reference via FK. Auto-assigned ids start at 1.
     */
    private suspend fun seed() {
        database.walletDao().upsert(
            WalletEntity(currency = Currency.CUP, name = "Efectivo"),
        )
        database.cardDao().upsert(
            CardEntity(
                maskedNumber = "123456******7890",
                bank = "POP",
                type = CardType.DEBIT,
                currency = Currency.USD,
                note = null,
                expirationDate = java.time.LocalDate.of(2028, 12, 31),
            ),
        )
        database.stashDao().upsert(
            StashEntity(
                name = "Vacation",
                createdAt = java.time.Instant.ofEpochMilli(0L),
                updatedAt = java.time.Instant.ofEpochMilli(0L),
                currency = Currency.USD,
            ),
        )
    }

    @Test
    fun `upsert then observeAll emits entity`() = runTest {
        seed()
        dao.upsert(
            anIncome(destinationCardId = 1L, dateTime = 1_000L, description = "Salario"),
        )
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Salario", items[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns entity after upsert`() = runTest {
        seed()
        val id = dao.upsert(anIncome(destinationCardId = 1L))
        val result = dao.getById(id)
        assertNotNull(result)
        assertEquals("Salary", result!!.description)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        assertNull(dao.getById(999L))
    }

    @Test
    fun `delete removes row`() = runTest {
        seed()
        val id = dao.upsert(anIncome(destinationCardId = 1L))
        dao.delete(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun `observeBySource filters by card destination`() = runTest {
        seed()
        dao.upsert(anIncome(destinationCardId = 1L, dateTime = 100L))
        dao.upsert(anIncome(destinationStashId = 1L, dateTime = 100L))

        val cardItems = dao.observeBySource("CARD", 1L, 1_000L).first()
        assertEquals(1, cardItems.size)
        assertEquals(1L, cardItems[0].destinationCardId)

        val stashItems = dao.observeBySource("STASH", 1L, 1_000L).first()
        assertEquals(1, stashItems.size)
        assertEquals(1L, stashItems[0].destinationStashId)
    }

    @Test
    fun `observeBySource for wallet returns wallet incomes only`() = runTest {
        seed()
        dao.upsert(anIncome(destinationWalletId = 1L, dateTime = 100L))
        dao.upsert(anIncome(destinationCardId = 1L, dateTime = 100L))

        val walletItems = dao.observeBySource("WALLET", null, 1_000L).first()
        assertEquals(1, walletItems.size)
        assertEquals(1L, walletItems[0].destinationWalletId)
        assertNull(walletItems[0].destinationCardId)
    }

    @Test
    fun `observeBySource respects asOf cutoff`() = runTest {
        seed()
        dao.upsert(anIncome(destinationCardId = 1L, dateTime = 50L))
        dao.upsert(anIncome(destinationCardId = 1L, dateTime = 200L))

        val items = dao.observeBySource("CARD", 1L, 100L).first()
        assertEquals(1, items.size)
        assertEquals(50L, items[0].dateTime)
    }

    @Test
    fun `observeByDateRange returns incomes within half-open range`() = runTest {
        seed()
        dao.upsert(anIncome(destinationWalletId = 1L, dateTime = 100L))
        dao.upsert(anIncome(destinationWalletId = 1L, dateTime = 150L))
        dao.upsert(anIncome(destinationWalletId = 1L, dateTime = 200L))
        dao.upsert(anIncome(destinationWalletId = 1L, dateTime = 250L))

        val items = dao.observeByDateRange(150L, 250L).first()
        // [150, 250): includes 150 and 200, excludes 100 and 250
        assertEquals(2, items.size)
        val times = items.map { it.dateTime }.sorted()
        assert(times.contains(150L))
        assert(times.contains(200L))
    }

    @Test
    fun `empty table returns empty list`() = runTest {
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(0, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun anIncome(
        destinationWalletId: Long? = null,
        destinationCardId: Long? = null,
        destinationStashId: Long? = null,
        dateTime: Long = 1_000L,
        amountCurrency: String = "CUP",
        description: String = "Salary",
    ) = IncomeEntity(
        amountMinor = 500_000L, // 5,000.00 in minor units
        amountCurrency = amountCurrency,
        description = description,
        dateTime = dateTime,
        note = null,
        destinationWalletId = destinationWalletId,
        destinationCardId = destinationCardId,
        destinationStashId = destinationStashId,
    )
}
