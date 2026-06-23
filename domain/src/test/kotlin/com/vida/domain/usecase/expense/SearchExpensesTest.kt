package com.vida.domain.usecase.expense

import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.ExpenseFilter
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.repository.ExpenseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class SearchExpensesTest {

    private val repo: ExpenseRepository = mockk()
    private val searchExpenses = SearchExpenses(repo)

    private val sampleExpenses = listOf(
        Expense(
            id = 1L,
            categoryId = 1L,
            amount = Money(BigDecimal("12.50"), Currency.CUP),
            description = "Lunch",
            dateTime = Instant.parse("2026-06-20T12:00:00Z"),
            sourceType = SourceType.WALLET,
        ),
    )

    @Test
    fun `delegates to repository with filter limit and offset`() = runTest {
        val filter = ExpenseFilter(
            dateFrom = Instant.parse("2026-06-01T00:00:00Z"),
            currency = Currency.CUP,
            searchQuery = "café",
        )
        val limit = 20
        val offset = 0

        coEvery { repo.searchExpenses(filter, limit, offset) } returns sampleExpenses

        val result = searchExpenses(filter, limit, offset)

        assertEquals(sampleExpenses, result)
        coVerify(exactly = 1) { repo.searchExpenses(filter, limit, offset) }
    }

    @Test
    fun `empty filter returns results from repository`() = runTest {
        val filter = ExpenseFilter()
        val limit = 10
        val offset = 20

        coEvery { repo.searchExpenses(filter, limit, offset) } returns emptyList()

        val result = searchExpenses(filter, limit, offset)

        assertEquals(emptyList<Expense>(), result)
        coVerify(exactly = 1) { repo.searchExpenses(filter, limit, offset) }
    }

    @Test
    fun `passes correct params when all fields set`() = runTest {
        val filter = ExpenseFilter(
            dateFrom = Instant.parse("2026-01-01T00:00:00Z"),
            dateTo = Instant.parse("2026-01-31T23:59:59Z"),
            categoryIds = setOf(1L, 2L),
            currency = Currency.USD,
            sourceType = SourceType.CARD,
            searchQuery = "supermercado",
        )
        val limit = 20
        val offset = 40

        coEvery { repo.searchExpenses(filter, limit, offset) } returns sampleExpenses

        val result = searchExpenses(filter, limit, offset)

        assertEquals(sampleExpenses, result)
        coVerify(exactly = 1) { repo.searchExpenses(filter, limit, offset) }
    }
}
