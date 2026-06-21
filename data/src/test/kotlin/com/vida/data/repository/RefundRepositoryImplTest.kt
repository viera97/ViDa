package com.vida.data.repository

import android.database.sqlite.SQLiteConstraintException
import app.cash.turbine.test
import com.vida.data.db.dao.RefundDao
import com.vida.data.db.entity.RefundEntity
import com.vida.data.mapper.RefundMapper
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Refund
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class RefundRepositoryImplTest {
    private val dao = mockk<RefundDao>(relaxed = true)
    private val mapper = RefundMapper
    private lateinit var repository: RefundRepositoryImpl

    @Before
    fun setUp() {
        repository = RefundRepositoryImpl(dao, mapper)
    }

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        val entity = anEntity()
        coEvery { dao.observeAll() } returns flowOf(listOf(entity))

        repository.getAll().test {
            val refunds = awaitItem()
            assertEquals(1, refunds.size)
            assertEquals("defective", refunds[0].reason)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns mapped refund when found`() = runTest {
        coEvery { dao.getById(1L) } returns anEntity()

        val refund = repository.getById(1L)
        assertEquals("defective", refund!!.reason)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(any()) } returns null
        assertNull(repository.getById(42L))
    }

    @Test
    fun `getByOriginalExpense delegates to dao`() = runTest {
        coEvery { dao.observeByOriginalExpense(any()) } returns flowOf(listOf(anEntity()))

        repository.getByOriginalExpense(5L).test {
            val refunds = awaitItem()
            assertEquals(1, refunds.size)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { dao.observeByOriginalExpense(5L) }
    }

    @Test
    fun `upsert delegates to dao and returns id on success`() = runTest {
        val refund = aRefund()
        val entity = mapper.toEntity(refund)
        coEvery { dao.insert(entity) } returns 9L

        val id = repository.upsert(refund)
        assertEquals(9L, id)
        coVerify { dao.insert(entity) }
    }

    @Test
    fun `upsert of existing refund calls dao update and returns the id`() = runTest {
        val refund = aRefund().copy(id = 7L)
        val entity = mapper.toEntity(refund)
        coEvery { dao.update(entity) } returns Unit

        val id = repository.upsert(refund)
        assertEquals(7L, id)
        coVerify { dao.update(entity) }
    }

    @Test
    fun `upsert rethrows SQLiteConstraintException as IllegalStateException`() = runTest {
        val refund = aRefund(originalExpenseId = 5L)
        val entity = mapper.toEntity(refund)
        coEvery { dao.insert(entity) } throws SQLiteConstraintException("UNIQUE constraint failed: refunds.original_expense_id")

        try {
            repository.upsert(refund)
            assert(false) { "Expected IllegalStateException" }
        } catch (e: IllegalStateException) {
            assertTrue(
                "Message must identify the expense: ${e.message}",
                e.message!!.contains("Refund already exists for expense 5"),
            )
        }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(1L)
        coVerify { dao.delete(1L) }
    }

    private fun anEntity() = RefundEntity(
        id = 1L,
        originalExpenseId = 5L,
        amountMinor = 2500L,
        amountCurrency = "USD",
        reason = "defective",
        dateTime = 5_000L,
        note = null,
    )

    private fun aRefund(originalExpenseId: Long = 5L) = Refund(
        id = 0L,
        originalExpenseId = originalExpenseId,
        amount = Money.of("25.00", Currency.USD),
        reason = "defective",
        dateTime = Instant.ofEpochMilli(5_000L),
        note = null,
    )
}
