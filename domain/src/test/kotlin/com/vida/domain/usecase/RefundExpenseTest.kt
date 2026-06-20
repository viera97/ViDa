package com.vida.domain.usecase.expense

import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.Money
import com.vida.domain.model.Refund
import com.vida.domain.model.SourceType
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.RefundRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class RefundExpenseTest {

    private val now: Instant = Instant.parse("2026-06-19T12:00:00Z")

    private fun originalExpense(
        id: Long = 42L,
        amount: Money = Money(BigDecimal("1000"), Currency.CUP),
    ): Expense = Expense(
        id = id,
        categoryId = 1L,
        amount = amount,
        description = "X",
        dateTime = now,
        sourceType = SourceType.CARD,
        sourceId = 5L,
    )

    @Test
    fun `partial refund succeeds and upserts Refund exactly once`() = runTest {
        val expenseRepo = mockk<ExpenseRepository>()
        val refundRepo = mockk<RefundRepository>()
        coEvery { expenseRepo.getById(42L) } returns originalExpense()
        coEvery { refundRepo.upsert(any()) } returns 99L

        val newId = RefundExpense(expenseRepo, refundRepo).invoke(
            originalExpenseId = 42L,
            refundAmount = Money(BigDecimal("300"), Currency.CUP),
            reason = "defective",
            now = now,
        )

        assertEquals(99L, newId)

        val captured = slot<Refund>()
        coVerify(exactly = 1) { refundRepo.upsert(capture(captured)) }
        assertEquals(42L, captured.captured.originalExpenseId)
        assertEquals(Money(BigDecimal("300"), Currency.CUP), captured.captured.amount)
        assertEquals("defective", captured.captured.reason)
        assertEquals(now, captured.captured.dateTime)
    }

    @Test
    fun `full refund equal to original succeeds`() = runTest {
        val expenseRepo = mockk<ExpenseRepository>()
        val refundRepo = mockk<RefundRepository>()
        coEvery { expenseRepo.getById(42L) } returns originalExpense()
        coEvery { refundRepo.upsert(any()) } returns 100L

        val newId = RefundExpense(expenseRepo, refundRepo).invoke(
            originalExpenseId = 42L,
            refundAmount = Money(BigDecimal("1000"), Currency.CUP),
            reason = "full refund",
            now = now,
        )

        assertEquals(100L, newId)
        coVerify(exactly = 1) { refundRepo.upsert(any()) }
    }

    @Test
    fun `refund exceeding original is rejected and does not upsert`() = runTest {
        val expenseRepo = mockk<ExpenseRepository>()
        val refundRepo = mockk<RefundRepository>(relaxed = true)
        coEvery { expenseRepo.getById(42L) } returns originalExpense()

        var thrown: Throwable? = null
        try {
            RefundExpense(expenseRepo, refundRepo).invoke(
                originalExpenseId = 42L,
                refundAmount = Money(BigDecimal("1500"), Currency.CUP),
                reason = "too much",
                now = now,
            )
        } catch (e: Throwable) {
            thrown = e
        }
        assertEquals(true, thrown is IllegalArgumentException)

        coVerify(exactly = 0) { refundRepo.upsert(any()) }
    }

    @Test
    fun `refund in different currency from original is rejected`() = runTest {
        val expenseRepo = mockk<ExpenseRepository>()
        val refundRepo = mockk<RefundRepository>(relaxed = true)
        coEvery { expenseRepo.getById(42L) } returns originalExpense()

        var thrown: Throwable? = null
        try {
            RefundExpense(expenseRepo, refundRepo).invoke(
                originalExpenseId = 42L,
                refundAmount = Money(BigDecimal("500"), Currency.USD),
                reason = "wrong currency",
                now = now,
            )
        } catch (e: Throwable) {
            thrown = e
        }
        assertEquals(true, thrown is IllegalArgumentException)

        coVerify(exactly = 0) { refundRepo.upsert(any()) }
    }

    @Test
    fun `missing original expense throws NoSuchElementException`() = runTest {
        val expenseRepo = mockk<ExpenseRepository>()
        val refundRepo = mockk<RefundRepository>(relaxed = true)
        coEvery { expenseRepo.getById(42L) } returns null

        var thrown: Throwable? = null
        try {
            RefundExpense(expenseRepo, refundRepo).invoke(
                originalExpenseId = 42L,
                refundAmount = Money(BigDecimal("100"), Currency.CUP),
                reason = "X",
                now = now,
            )
        } catch (e: Throwable) {
            thrown = e
        }
        assertEquals(true, thrown is NoSuchElementException)

        coVerify(exactly = 0) { refundRepo.upsert(any()) }
    }

    @Test
    fun `blank reason is rejected before consulting repos`() = runTest {
        val expenseRepo = mockk<ExpenseRepository>(relaxed = true)
        val refundRepo = mockk<RefundRepository>(relaxed = true)

        var thrown: Throwable? = null
        try {
            RefundExpense(expenseRepo, refundRepo).invoke(
                originalExpenseId = 42L,
                refundAmount = Money(BigDecimal("100"), Currency.CUP),
                reason = "  ",
                now = now,
            )
        } catch (e: Throwable) {
            thrown = e
        }
        assertEquals(true, thrown is IllegalArgumentException)

        coVerify(exactly = 0) { expenseRepo.getById(any()) }
        coVerify(exactly = 0) { refundRepo.upsert(any()) }
    }
}
