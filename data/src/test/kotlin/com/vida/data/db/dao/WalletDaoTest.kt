package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.WalletEntity
import com.vida.domain.model.Currency
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
class WalletDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: WalletDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.walletDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `get returns null when empty`() = runTest {
        assertNull(dao.get())
    }

    @Test
    fun `upsert then get returns wallet`() = runTest {
        dao.upsert(WalletEntity(currency = Currency.CUP))
        val result = dao.get()
        assertNotNull(result)
        assertEquals(Currency.CUP, result!!.currency)
    }

    @Test
    fun `upsert replaces existing singleton`() = runTest {
        dao.upsert(WalletEntity(currency = Currency.CUP))
        dao.upsert(WalletEntity(currency = Currency.USD))
        val result = dao.get()
        assertEquals(Currency.USD, result!!.currency)
    }
}
