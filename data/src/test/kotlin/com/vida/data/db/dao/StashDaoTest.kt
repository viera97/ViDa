package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.StashEntity
import com.vida.domain.model.Currency
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StashDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: StashDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.stashDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert then observeAll emits entity`() = runTest {
        val entity = aStashEntity()
        dao.upsert(entity)

        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Vacation Fund", items[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns entity after upsert`() = runTest {
        val id = dao.upsert(aStashEntity())
        val result = dao.getById(id)
        assertEquals("Vacation Fund", result!!.name)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        assertNull(dao.getById(999L))
    }

    @Test
    fun `upsert overwrites existing row`() = runTest {
        val id = dao.upsert(aStashEntity())
        val updated = aStashEntity().copy(id = id, name = "Emergency Fund")
        dao.upsert(updated)

        val result = dao.getById(id)
        assertEquals("Emergency Fund", result!!.name)
    }

    @Test
    fun `delete removes row`() = runTest {
        val id = dao.upsert(aStashEntity())
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

    private fun aStashEntity() = StashEntity(
        name = "Vacation Fund",
        createdAt = Instant.ofEpochMilli(1000),
        updatedAt = Instant.ofEpochMilli(1000),
        currency = Currency.USD,
    )
}
