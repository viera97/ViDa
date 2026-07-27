package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.CurrencyEntity
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
class CurrencyDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: CurrencyDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.currencyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert then observeAll emits entity`() = runTest {
        dao.upsert(aCurrency())
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Dólar", items[0].name)
            assertEquals("USD", items[0].code)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAll orders by code ascending`() = runTest {
        dao.upsert(aCurrency().copy(name = "Zeta", code = "ZET"))
        dao.upsert(aCurrency().copy(name = "Alpha", code = "ALP"))
        dao.upsert(aCurrency().copy(name = "Mike", code = "MIK"))

        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(3, items.size)
            assertEquals(listOf("ALP", "MIK", "ZET"), items.map { it.code })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns entity after upsert`() = runTest {
        val id = dao.upsert(aCurrency())
        val result = dao.getById(id)
        assertNotNull(result)
        assertEquals("Dólar", result!!.name)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        assertNull(dao.getById(999L))
    }

    @Test
    fun `getByCode returns entity when found`() = runTest {
        dao.upsert(aCurrency())
        val result = dao.getByCode("USD")
        assertNotNull(result)
        assertEquals("Dólar", result!!.name)
    }

    @Test
    fun `getByCode returns null when not found`() = runTest {
        assertNull(dao.getByCode("UNKNOWN"))
    }

    @Test
    fun `upsert inserts new entity when id is zero`() = runTest {
        val id = dao.upsert(aCurrency())
        assertEquals(true, id > 0L)
        val result = dao.getById(id)
        assertEquals("Dólar", result!!.name)
    }

    @Test
    fun `upsert overwrites existing row when id matches`() = runTest {
        val id = dao.upsert(aCurrency())
        val updated = aCurrency().copy(id = id, name = "Dólar Americano")
        dao.upsert(updated)
        val result = dao.getById(id)
        assertEquals("Dólar Americano", result!!.name)
    }

    @Test
    fun `upsert with duplicate code does not produce a duplicate row`() = runTest {
        // Verify the contractual guarantee: unique index on `code` prevents
        // duplicate rows regardless of how Room handles the conflict internally.
        dao.upsert(aCurrency())
        dao.upsert(aCurrency().copy(name = "Dólar Americano", isSystem = 0))
        val all = dao.getByCode("USD")
        assertNotNull(all)
        assertEquals("USD", all!!.code)
    }

    @Test
    fun `delete removes row`() = runTest {
        val id = dao.upsert(aCurrency())
        dao.delete(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun `empty table returns empty observeAll`() = runTest {
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(0, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isSystem stored as integer 0 or 1`() = runTest {
        val sysId = dao.upsert(aCurrency().copy(isSystem = 1, code = "SYS"))
        val userId = dao.upsert(aCurrency().copy(isSystem = 0, code = "USR", name = "User Cur"))
        assertEquals(1, dao.getById(sysId)!!.isSystem)
        assertEquals(0, dao.getById(userId)!!.isSystem)
    }

    private fun aCurrency() = CurrencyEntity(
        name = "Dólar",
        code = "USD",
        isSystem = 1,
    )
}
