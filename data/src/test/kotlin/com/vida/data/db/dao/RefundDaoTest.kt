package com.vida.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vida.data.db.AppDatabase
import com.vida.data.db.entity.CategoryEntity
import com.vida.data.db.entity.ExpenseEntity
import com.vida.data.db.entity.RefundEntity
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RefundDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: RefundDao
    private var expenseId: Long = 0L

    @Before
    fun setUp() = kotlinx.coroutines.runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.refundDao()
        // FK prerequisites: a category and an expense.
        database.categoryDao().upsert(CategoryEntity(name = "Comida", color = 0, icon = null, isSystem = 0))
        expenseId = database.expenseDao().upsert(
            ExpenseEntity(
                categoryId = 1L,
                amountMinor = 1000L,
                amountCurrency = "CUP",
                realAmountMinor = null,
                realAmountCurrency = null,
                description = "Lunch",
                dateTime = 1_000L,
                note = null,
                sourceWalletId = 1L,
                sourceCardId = null,
                sourceStashId = null,
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert then observeAll emits entity`() = runTest {
        dao.insert(aRefund())
        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("defective", items[0].reason)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns entity after upsert`() = runTest {
        val id = dao.insert(aRefund())
        val result = dao.getById(id)
        assertEquals("defective", result!!.reason)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        assertNull(dao.getById(999L))
    }

    @Test
    fun `observeByOriginalExpense returns refund for the expense`() = runTest {
        dao.insert(aRefund())
        val items = dao.observeByOriginalExpense(expenseId).first()
        assertEquals(1, items.size)
        assertEquals(expenseId, items[0].originalExpenseId)

        val none = dao.observeByOriginalExpense(99L).first()
        assertTrue(none.isEmpty())
    }

    @Test
    fun `unique constraint rejects second refund for same expense`() = runTest {
        dao.insert(aRefund())
        var threw = false
        try {
            dao.insert(aRefund(note = "second attempt"))
        } catch (_: Exception) {
            threw = true
        }
        assertTrue("Expected a constraint exception on duplicate original_expense_id", threw)

        // Original refund remains unchanged.
        val items = dao.observeByOriginalExpense(expenseId).first()
        assertEquals(1, items.size)
    }

    @Test
    fun `delete removes row`() = runTest {
        val id = dao.insert(aRefund())
        dao.delete(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun `deleting the expense cascades to the refund`() = runTest {
        dao.insert(aRefund())
        database.expenseDao().delete(expenseId)

        val items = dao.observeByOriginalExpense(expenseId).first()
        assertTrue("Refund should be removed when its expense is deleted (CASCADE)", items.isEmpty())
    }

    private fun aRefund(note: String? = null) = RefundEntity(
        originalExpenseId = expenseId,
        amountMinor = 2500L,
        amountCurrency = "USD",
        reason = "defective",
        dateTime = 5_000L,
        note = note,
    )
}
