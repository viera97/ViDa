package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.CardEntity
import com.vida.domain.model.CardType
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CardDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: CardDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.cardDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert then observeAll emits entity`() = runTest {
        val entity = aCardEntity()
        dao.upsert(entity)

        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Test Bank", items[0].bank)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns entity after upsert`() = runTest {
        val id = dao.upsert(aCardEntity())
        val result = dao.getById(id)
        assertEquals("Test Bank", result!!.bank)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        assertNull(dao.getById(999L))
    }

    @Test
    fun `upsert overwrites existing row`() = runTest {
        val id = dao.upsert(aCardEntity())
        val updated = aCardEntity().copy(id = id, bank = "Updated Bank")
        dao.upsert(updated)

        val result = dao.getById(id)
        assertEquals("Updated Bank", result!!.bank)
    }

    @Test
    fun `delete removes row`() = runTest {
        val id = dao.upsert(aCardEntity())
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

    private fun aCardEntity() = CardEntity(
        maskedNumber = "123456******7890",
        bank = "Test Bank",
        type = CardType.DEBIT,
        currency = Currency.CUP,
        note = null,
        expirationDate = LocalDate.of(2028, 12, 31),
    )
}
