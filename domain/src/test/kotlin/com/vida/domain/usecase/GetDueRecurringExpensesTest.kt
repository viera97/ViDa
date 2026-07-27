package com.vida.domain.usecase.recurring

import com.vida.domain.model.Currency
import com.vida.domain.model.Frequency
import com.vida.domain.model.Money
import com.vida.domain.model.RecurringExpense
import com.vida.domain.model.SourceType
import com.vida.domain.repository.RecurringExpenseRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class GetDueRecurringExpensesTest {

    private val oneCup: Money = Money(BigDecimal.ONE, Currency.CUP)

    private fun template(
        id: Long,
        frequency: Frequency,
        lastGenerated: LocalDate?,
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
        endDate: LocalDate? = null,
        isActive: Boolean = true,
    ): RecurringExpense = RecurringExpense(
        id = id,
        amount = oneCup,
        currency = "CUP",
        categoryId = 1L,
        sourceType = SourceType.WALLET,
        description = "X",
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        lastGeneratedDate = lastGenerated,
        isActive = isActive,
    )

    @Test
    fun `daily last-gen yesterday is due when asOf is today`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        coEvery { repo.getAll() } returns flowOf(listOf(template(1L, Frequency.DAILY, LocalDate.of(2026, 6, 19))))

        val due = GetDueRecurringExpenses(repo).invoke(LocalDate.of(2026, 6, 20)).first()

        assertEquals(listOf(1L), due.map { it.id })
    }

    @Test
    fun `daily last-gen today is not due (next-due is tomorrow)`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        coEvery { repo.getAll() } returns flowOf(listOf(template(1L, Frequency.DAILY, LocalDate.of(2026, 6, 19))))

        val due = GetDueRecurringExpenses(repo).invoke(LocalDate.of(2026, 6, 19)).first()

        assertTrue(due.isEmpty())
    }

    @Test
    fun `weekly last-gen week-ago is due when asOf is exactly a week later`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        coEvery { repo.getAll() } returns flowOf(listOf(template(1L, Frequency.WEEKLY, LocalDate.of(2026, 6, 12))))

        val due = GetDueRecurringExpenses(repo).invoke(LocalDate.of(2026, 6, 19)).first()

        assertEquals(listOf(1L), due.map { it.id })
    }

    @Test
    fun `monthly last-gen 2026-05-31 is due on 2026-06-30 (month-end clamp)`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        coEvery { repo.getAll() } returns flowOf(listOf(template(1L, Frequency.MONTHLY, LocalDate.of(2026, 5, 31))))

        val due = GetDueRecurringExpenses(repo).invoke(LocalDate.of(2026, 6, 30)).first()

        // 2026-05-31 + 1 month = 2026-06-30 (Java LocalDate clamps month-end).
        // nextDueDate (2026-06-30) <= asOf (2026-06-30) → due.
        assertEquals(listOf(1L), due.map { it.id })
    }

    @Test
    fun `monthly last-gen 2026-01-31 is due on 2026-02-28 (clamp across short month)`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        coEvery { repo.getAll() } returns flowOf(listOf(template(1L, Frequency.MONTHLY, LocalDate.of(2026, 1, 31))))

        val due = GetDueRecurringExpenses(repo).invoke(LocalDate.of(2026, 2, 28)).first()

        // 2026-01-31 + 1 month = 2026-02-28 (clamp). nextDueDate (2026-02-28) <= asOf → due.
        assertEquals(listOf(1L), due.map { it.id })
    }

    @Test
    fun `yearly last-gen 2024-02-29 (leap) is due on 2025-02-28 (clamp to non-leap)`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        coEvery { repo.getAll() } returns flowOf(
            listOf(
                template(
                    id = 1L,
                    frequency = Frequency.YEARLY,
                    lastGenerated = LocalDate.of(2024, 2, 29),
                    startDate = LocalDate.of(2024, 2, 29),
                ),
            ),
        )

        val due = GetDueRecurringExpenses(repo).invoke(LocalDate.of(2025, 2, 28)).first()

        // 2024-02-29 + 1 year = 2025-02-28 (Java LocalDate clamps leap day). nextDueDate <= asOf → due.
        assertEquals(listOf(1L), due.map { it.id })
    }

    @Test
    fun `never-generated template with past startDate is due`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        coEvery { repo.getAll() } returns flowOf(
            listOf(template(1L, Frequency.MONTHLY, lastGenerated = null, startDate = LocalDate.of(2026, 1, 1))),
        )

        val due = GetDueRecurringExpenses(repo).invoke(LocalDate.of(2026, 6, 20)).first()

        // nextDueDate falls back to startDate (2026-01-01); startDate <= asOf → due.
        assertEquals(listOf(1L), due.map { it.id })
    }

    @Test
    fun `never-generated template with future startDate is not due`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        coEvery { repo.getAll() } returns flowOf(
            listOf(template(1L, Frequency.MONTHLY, lastGenerated = null, startDate = LocalDate.of(2026, 7, 1))),
        )

        val due = GetDueRecurringExpenses(repo).invoke(LocalDate.of(2026, 6, 20)).first()

        // nextDueDate falls back to startDate (2026-07-01); startDate > asOf → not due.
        assertTrue(due.isEmpty())
    }

    @Test
    fun `inactive template is never due regardless of date`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        coEvery { repo.getAll() } returns flowOf(
            listOf(template(1L, Frequency.DAILY, LocalDate.of(2026, 6, 1), isActive = false)),
        )

        val due = GetDueRecurringExpenses(repo).invoke(LocalDate.of(2026, 6, 30)).first()

        assertTrue(due.isEmpty())
    }

    @Test
    fun `template whose endDate has passed is not due`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        coEvery { repo.getAll() } returns flowOf(
            listOf(
                template(
                    1L,
                    Frequency.DAILY,
                    LocalDate.of(2026, 6, 1),
                    startDate = LocalDate.of(2026, 1, 1),
                    endDate = LocalDate.of(2026, 6, 5),
                ),
            ),
        )

        val due = GetDueRecurringExpenses(repo).invoke(LocalDate.of(2026, 6, 30)).first()

        // asOf (2026-06-30) > endDate (2026-06-05) → template is past its end → not due.
        assertTrue(due.isEmpty())
    }

    @Test
    fun `mix of templates returns only those that are due`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        val longAgo = LocalDate.of(2024, 1, 1) // startDate well before any candidate
        coEvery { repo.getAll() } returns flowOf(
            listOf(
                template(1L, Frequency.DAILY, LocalDate.of(2026, 6, 19), startDate = longAgo), // due today
                template(2L, Frequency.WEEKLY, LocalDate.of(2026, 6, 18), startDate = longAgo), // next-due 2026-06-25, not due
                template(3L, Frequency.MONTHLY, LocalDate.of(2026, 5, 15), startDate = longAgo), // next-due 2026-06-15, due
                template(4L, Frequency.YEARLY, LocalDate.of(2025, 6, 15), startDate = longAgo), // next-due 2026-06-15, due
                template(5L, Frequency.DAILY, LocalDate.of(2026, 6, 19), isActive = false, startDate = longAgo), // inactive
                template(6L, Frequency.MONTHLY, lastGenerated = null, startDate = LocalDate.of(2026, 7, 1)), // future
            ),
        )

        val due = GetDueRecurringExpenses(repo).invoke(LocalDate.of(2026, 6, 20)).first()

        assertEquals(listOf(1L, 3L, 4L), due.map { it.id }.sorted())
    }
}
