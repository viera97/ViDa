package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.CardEntity
import com.vida.data.db.entity.CategoryEntity
import com.vida.data.db.entity.ExpenseEntity
import com.vida.data.db.entity.StashEntity
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
class ExpenseDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ExpenseDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.expenseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert then observeAll emits entity`() = runTest {
        seed()
        dao.upsert(anExpense(sourceCardId = 1L, dateTime = 1_000L))
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Lunch", items[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns entity after upsert`() = runTest {
        seed()
        val id = dao.upsert(anExpense(sourceCardId = 1L))
        val result = dao.getById(id)
        assertEquals("Lunch", result!!.description)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        assertNull(dao.getById(999L))
    }

    @Test
    fun `delete removes row`() = runTest {
        seed()
        val id = dao.upsert(anExpense(sourceCardId = 1L))
        dao.delete(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun `observeBySource filters by card source`() = runTest {
        seed()
        dao.upsert(anExpense(sourceCardId = 1L, dateTime = 100L))
        dao.upsert(anExpense(sourceStashId = 1L, dateTime = 100L))

        val cardItems = dao.observeBySource("CARD", 1L, 1_000L).first()
        assertEquals(1, cardItems.size)
        assertEquals(1L, cardItems[0].sourceCardId)

        val stashItems = dao.observeBySource("STASH", 1L, 1_000L).first()
        assertEquals(1, stashItems.size)
        assertEquals(1L, stashItems[0].sourceStashId)
    }

    @Test
    fun `observeBySource for wallet returns wallet expenses only`() = runTest {
        seed()
        dao.upsert(anExpense(sourceWalletId = 1L, dateTime = 100L))
        dao.upsert(anExpense(sourceCardId = 1L, dateTime = 100L))

        val walletItems = dao.observeBySource("WALLET", null, 1_000L).first()
        assertEquals(1, walletItems.size)
        assertEquals(1L, walletItems[0].sourceWalletId)
        assertNull(walletItems[0].sourceCardId)
    }

    @Test
    fun `observeBySource respects asOf cutoff`() = runTest {
        seed()
        dao.upsert(anExpense(sourceCardId = 1L, dateTime = 50L))
        dao.upsert(anExpense(sourceCardId = 1L, dateTime = 200L))

        val items = dao.observeBySource("CARD", 1L, 100L).first()
        assertEquals(1, items.size)
        assertEquals(50L, items[0].dateTime)
    }

    @Test
    fun `observeByCategory filters by category id`() = runTest {
        seed()
        dao.upsert(anExpense(categoryId = 1L, sourceWalletId = 1L, dateTime = 100L))
        dao.upsert(anExpense(categoryId = 2L, sourceWalletId = 1L, dateTime = 100L))

        val cat1 = dao.observeByCategory(1L, 1_000L).first()
        assertEquals(1, cat1.size)
        val cat2 = dao.observeByCategory(2L, 1_000L).first()
        assertEquals(1, cat2.size)
        val cat3 = dao.observeByCategory(99L, 1_000L).first()
        assertTrue(cat3.isEmpty())
    }

    @Test
    fun `observeByDateRange returns expenses within half-open range`() = runTest {
        seed()
        dao.upsert(anExpense(sourceWalletId = 1L, dateTime = 100L))
        dao.upsert(anExpense(sourceWalletId = 1L, dateTime = 150L))
        dao.upsert(anExpense(sourceWalletId = 1L, dateTime = 200L))
        dao.upsert(anExpense(sourceWalletId = 1L, dateTime = 250L))

        val items = dao.observeByDateRange(150L, 250L).first()
        // [150, 250): includes 150 and 200, excludes 100 and 250
        assertEquals(2, items.size)
        val times = items.map { it.dateTime }.sorted()
        assertTrue(times.contains(150L))
        assertTrue(times.contains(200L))
    }

    @Test
    fun `empty table returns empty list`() = runTest {
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(0, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun seed() {
        database.categoryDao().upsert(CategoryEntity(name = "Comida", color = 0, icon = null, isSystem = 0))
        database.categoryDao().upsert(CategoryEntity(name = "Transporte", color = 0, icon = null, isSystem = 0))
        database.cardDao().upsert(
            CardEntity(
                maskedNumber = "123456******7890",
                bank = "POP",
                type = CardType.DEBIT,
                currency = Currency.USD,
                note = null,
                expirationDate = LocalDate.of(2028, 12, 31),
            ),
        )
        database.stashDao().upsert(
            StashEntity(
                name = "Emergency",
                createdAt = java.time.Instant.ofEpochMilli(0L),
                updatedAt = java.time.Instant.ofEpochMilli(0L),
                currency = Currency.USD,
            ),
        )
    }

    private fun anExpense(
        categoryId: Long = 1L,
        sourceWalletId: Long? = null,
        sourceCardId: Long? = null,
        sourceStashId: Long? = null,
        dateTime: Long = 1_000L,
    ) = ExpenseEntity(
        categoryId = categoryId,
        amountMinor = 1234L,
        amountCurrency = "CUP",
        realAmountMinor = null,
        realAmountCurrency = null,
        description = "Lunch",
        dateTime = dateTime,
        note = null,
        sourceWalletId = sourceWalletId,
        sourceCardId = sourceCardId,
        sourceStashId = sourceStashId,
    )
}
