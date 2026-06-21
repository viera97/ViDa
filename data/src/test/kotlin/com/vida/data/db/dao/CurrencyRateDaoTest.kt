package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.CurrencyRateEntity
import com.vida.domain.model.Currency
import kotlinx.coroutines.flow.first
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
class CurrencyRateDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: CurrencyRateDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.currencyRateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert then observeAll emits entity`() = runTest {
        dao.upsert(aRate(Currency.USD, Currency.CUP, 24.0, 100L))
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(24.0, items[0].rate, 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById not available but getRate returns latest before cutoff`() = runTest {
        dao.upsert(aRate(Currency.USD, Currency.CUP, 24.0, 100L))
        dao.upsert(aRate(Currency.USD, Currency.CUP, 24.5, 200L))
        dao.upsert(aRate(Currency.USD, Currency.CUP, 25.0, 300L))

        val at250 = dao.getRate("USD", "CUP", 250L)
        assertEquals(200L, at250!!.effectiveDate)
        assertEquals(24.5, at250.rate, 0.0001)

        val at300 = dao.getRate("USD", "CUP", 300L)
        assertEquals(300L, at300!!.effectiveDate)

        val at350 = dao.getRate("USD", "CUP", 350L)
        assertEquals(300L, at350!!.effectiveDate)
    }

    @Test
    fun `getRate returns null when cutoff is before any snapshot`() = runTest {
        dao.upsert(aRate(Currency.USD, Currency.CUP, 24.0, 100L))
        assertNull(dao.getRate("USD", "CUP", 50L))
    }

    @Test
    fun `getRate returns null for missing pair`() = runTest {
        dao.upsert(aRate(Currency.USD, Currency.CUP, 24.0, 100L))
        assertNull(dao.getRate("USD", "MLC", 500L))
        assertNull(dao.getRate("MLC", "USD", 500L))
    }

    @Test
    fun `observeRateHistory returns snapshots ordered newest first`() = runTest {
        dao.upsert(aRate(Currency.USD, Currency.CUP, 24.0, 100L))
        dao.upsert(aRate(Currency.USD, Currency.CUP, 24.5, 200L))
        dao.upsert(aRate(Currency.USD, Currency.CUP, 25.0, 300L))

        val history = dao.observeRateHistory("USD", "CUP").first()
        assertEquals(3, history.size)
        assertEquals(300L, history[0].effectiveDate)
        assertEquals(200L, history[1].effectiveDate)
        assertEquals(100L, history[2].effectiveDate)
    }

    @Test
    fun `observeRateHistory empty for missing pair`() = runTest {
        val history = dao.observeRateHistory("USD", "MLC").first()
        assertEquals(0, history.size)
    }

    @Test
    fun `delete removes row`() = runTest {
        val id = dao.upsert(aRate(Currency.USD, Currency.CUP, 24.0, 100L))
        dao.delete(id)
        assertNull(dao.getRate("USD", "CUP", 200L))
    }

    @Test
    fun `empty table returns empty observeAll`() = runTest {
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(0, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun aRate(from: Currency, to: Currency, rate: Double, effectiveDate: Long) =
        CurrencyRateEntity(
            fromCurrency = from,
            toCurrency = to,
            rate = rate,
            effectiveDate = effectiveDate,
        )
}
