package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.BankEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BankDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: BankDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.bankDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert then observeAll emits entity`() = runTest {
        dao.upsert(aBank())
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Bandec", items[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns entity after upsert`() = runTest {
        val id = dao.upsert(aBank())
        val result = dao.getById(id)
        assertEquals("Bandec", result!!.name)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        assertNull(dao.getById(999L))
    }

    @Test
    fun `upsert overwrites existing row`() = runTest {
        val id = dao.upsert(aBank())
        val updated = aBank().copy(id = id, name = "Banco Bandec")
        dao.upsert(updated)
        val result = dao.getById(id)
        assertEquals("Banco Bandec", result!!.name)
    }

    @Test
    fun `delete removes row`() = runTest {
        val id = dao.upsert(aBank())
        dao.delete(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun `empty table returns empty list`() = runTest {
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(0, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isSystem stored as integer 0 or 1`() = runTest {
        val sysId = dao.upsert(aBank().copy(isSystem = 1))
        val userId = dao.upsert(aBank().copy(isSystem = 0, name = "User"))
        assertEquals(1, dao.getById(sysId)!!.isSystem)
        assertEquals(0, dao.getById(userId)!!.isSystem)
    }

    @Test
    fun `getByName returns entity when found`() = runTest {
        val id = dao.upsert(aBank())
        val result = dao.getByName("Bandec")
        assertEquals(id, result!!.id)
    }

    @Test
    fun `getByName returns null when not found`() = runTest {
        assertNull(dao.getByName("NonExistent"))
    }

    private fun aBank() = BankEntity(
        name = "Bandec",
        color = 0xFF8E0509.toInt(),
        isSystem = 1,
    )
}
