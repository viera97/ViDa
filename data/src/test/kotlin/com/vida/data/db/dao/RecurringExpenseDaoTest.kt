package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.CategoryEntity
import com.vida.data.db.entity.RecurringExpenseEntity
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
class RecurringExpenseDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: RecurringExpenseDao

    @Before
    fun setUp() = kotlinx.coroutines.runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.recurringExpenseDao()
        database.categoryDao().upsert(CategoryEntity(name = "Comida", color = 0, icon = null, isSystem = 0))
        Unit
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert then observeAll emits entity`() = runTest {
        dao.upsert(aRecurring(startDate = LocalDate.of(2026, 1, 1).toEpochDay()))
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Netflix", items[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns entity after upsert`() = runTest {
        val id = dao.upsert(aRecurring(startDate = LocalDate.of(2026, 1, 1).toEpochDay()))
        val result = dao.getById(id)
        assertEquals("MONTHLY", result!!.frequency)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        assertNull(dao.getById(999L))
    }

    @Test
    fun `delete removes row`() = runTest {
        val id = dao.upsert(aRecurring(startDate = LocalDate.of(2026, 1, 1).toEpochDay()))
        dao.delete(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun `observeActive returns only active templates within date window`() = runTest {
        // active, started before asOf, no end date
        dao.upsert(aRecurring(startDate = epochDay(2026, 1, 1), isActive = 1))
        // active, started after asOf (outside window)
        dao.upsert(aRecurring(startDate = epochDay(2026, 6, 1), isActive = 1))
        // inactive, started before asOf
        dao.upsert(aRecurring(startDate = epochDay(2026, 1, 1), isActive = 0))

        val asOf = epochDay(2026, 3, 15)
        val items = dao.observeActive(asOf).first()
        assertEquals(1, items.size)
        assertEquals(epochDay(2026, 1, 1), items[0].startDate)
    }

    @Test
    fun `observeActive respects end date boundary`() = runTest {
        // active, start before asOf, end after asOf → included
        dao.upsert(aRecurring(
            startDate = epochDay(2026, 1, 1),
            endDate = epochDay(2026, 6, 1),
            isActive = 1,
        ))
        // active, start before asOf, end before asOf → excluded
        dao.upsert(aRecurring(
            startDate = epochDay(2026, 1, 1),
            endDate = epochDay(2026, 2, 1),
            isActive = 1,
        ))

        val asOf = epochDay(2026, 3, 15)
        val items = dao.observeActive(asOf).first()
        assertEquals(1, items.size)
        assertEquals(epochDay(2026, 6, 1), items[0].endDate)
    }

    @Test
    fun `deactivate sets is_active to 0`() = runTest {
        val id = dao.upsert(aRecurring(startDate = epochDay(2026, 1, 1), isActive = 1))
        dao.deactivate(id)
        val entity = dao.getById(id)
        assertEquals(0, entity!!.isActive)

        val asOf = epochDay(2026, 3, 15)
        val active = dao.observeActive(asOf).first()
        assertTrue("Deactivated expense should not appear in observeActive", active.isEmpty())
    }

    @Test
    fun `empty table returns empty list`() = runTest {
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(0, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun epochDay(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).toEpochDay()

    private fun aRecurring(
        startDate: Long,
        endDate: Long? = null,
        isActive: Int = 1,
    ) = RecurringExpenseEntity(
        categoryId = 1L,
        amountMinor = 5000L,
        amountCurrency = "CUP",
        description = "Netflix",
        frequency = "MONTHLY",
        startDate = startDate,
        endDate = endDate,
        lastGeneratedDate = null,
        isActive = isActive,
        sourceWalletId = 1L,
        sourceCardId = null,
        sourceStashId = null,
    )
}
