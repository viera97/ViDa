package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.CategoryEntity
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
class CategoryDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: CategoryDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.categoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert then observeAll emits entity`() = runTest {
        dao.upsert(aCategory())
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Comida", items[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns entity after upsert`() = runTest {
        val id = dao.upsert(aCategory())
        val result = dao.getById(id)
        assertEquals("Comida", result!!.name)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        assertNull(dao.getById(999L))
    }

    @Test
    fun `upsert overwrites existing row`() = runTest {
        val id = dao.upsert(aCategory())
        val updated = aCategory().copy(id = id, name = "Bebidas")
        dao.upsert(updated)
        val result = dao.getById(id)
        assertEquals("Bebidas", result!!.name)
    }

    @Test
    fun `delete removes row`() = runTest {
        val id = dao.upsert(aCategory())
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
        val sysId = dao.upsert(aCategory().copy(isSystem = 1))
        val userId = dao.upsert(aCategory().copy(isSystem = 0, name = "User"))
        assertEquals(1, dao.getById(sysId)!!.isSystem)
        assertEquals(0, dao.getById(userId)!!.isSystem)
    }

    private fun aCategory() = CategoryEntity(
        name = "Comida",
        color = 0xFFE57373.toInt(),
        icon = "restaurant",
        isSystem = 0,
    )
}
