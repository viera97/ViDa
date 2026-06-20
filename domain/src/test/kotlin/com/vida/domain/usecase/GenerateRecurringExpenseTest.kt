package com.vida.domain.usecase.recurring

import com.vida.domain.model.Currency
import com.vida.domain.model.Frequency
import com.vida.domain.model.Money
import com.vida.domain.model.RecurringExpense
import com.vida.domain.model.SourceType
import com.vida.domain.repository.RecurringExpenseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class GenerateRecurringExpenseTest {

    private val asOf: LocalDate = LocalDate.of(2026, 6, 20)

    private fun monthlyTemplate(
        id: Long = 10L,
        lastGenerated: LocalDate? = LocalDate.of(2026, 5, 15),
        isActive: Boolean = true,
        sourceType: SourceType = SourceType.CARD,
        sourceId: Long? = 5L,
    ): RecurringExpense = RecurringExpense(
        id = id,
        amount = Money(BigDecimal("2000"), Currency.CUP),
        currency = Currency.CUP,
        categoryId = 3L,
        sourceType = sourceType,
        sourceId = sourceId,
        description = "Alquiler",
        frequency = Frequency.MONTHLY,
        startDate = LocalDate.of(2026, 1, 1),
        lastGeneratedDate = lastGenerated,
        isActive = isActive,
    )

    @Test
    fun `valid template updates lastGeneratedDate to asOf via repo upsert`() = runTest {
        val repo = mockk<RecurringExpenseRepository>(relaxed = true)
        coEvery { repo.getById(10L) } returns monthlyTemplate(lastGenerated = LocalDate.of(2026, 5, 15))

        val returnedId = GenerateRecurringExpense(repo).invoke(10L, asOf)

        // v1 returns 0L: the use case only updates lastGeneratedDate; the actual Expense row is
        // created by the caller via RecordExpense (see KDoc on GenerateRecurringExpense).
        assertEquals(0L, returnedId)

        val captured = slot<RecurringExpense>()
        coVerify(exactly = 1) { repo.upsert(capture(captured)) }
        assertEquals(LocalDate.of(2026, 6, 20), captured.captured.lastGeneratedDate)
        assertEquals(10L, captured.captured.id)
        // Other fields are preserved from the template:
        assertEquals(BigDecimal("2000"), captured.captured.amount.amount)
        assertEquals(Currency.CUP, captured.captured.amount.currency)
        assertEquals(3L, captured.captured.categoryId)
        assertEquals("Alquiler", captured.captured.description)
        assertEquals(Frequency.MONTHLY, captured.captured.frequency)
        assertEquals(SourceType.CARD, captured.captured.sourceType)
        assertEquals(5L, captured.captured.sourceId)
    }

    @Test
    fun `not-found template throws NoSuchElementException and does not upsert`() = runTest {
        val repo = mockk<RecurringExpenseRepository>()
        coEvery { repo.getById(99L) } returns null

        var thrown: Throwable? = null
        try {
            GenerateRecurringExpense(repo).invoke(99L, asOf)
        } catch (e: Throwable) {
            thrown = e
        }
        assertEquals(true, thrown is NoSuchElementException)

        coVerify(exactly = 0) { repo.upsert(any()) }
    }

    @Test
    fun `first-ever generation (lastGeneratedDate null) still updates to asOf`() = runTest {
        val repo = mockk<RecurringExpenseRepository>(relaxed = true)
        coEvery { repo.getById(10L) } returns monthlyTemplate(lastGenerated = null)

        GenerateRecurringExpense(repo).invoke(10L, asOf)

        val captured = slot<RecurringExpense>()
        coVerify(exactly = 1) { repo.upsert(capture(captured)) }
        assertEquals(LocalDate.of(2026, 6, 20), captured.captured.lastGeneratedDate)
    }

    @Test
    fun `wallet source template is updated with null sourceId preserved`() = runTest {
        val repo = mockk<RecurringExpenseRepository>(relaxed = true)
        coEvery { repo.getById(10L) } returns monthlyTemplate(
            sourceType = SourceType.WALLET,
            sourceId = null,
        )

        GenerateRecurringExpense(repo).invoke(10L, asOf)

        val captured = slot<RecurringExpense>()
        coVerify(exactly = 1) { repo.upsert(capture(captured)) }
        assertEquals(SourceType.WALLET, captured.captured.sourceType)
        assertEquals(null, captured.captured.sourceId)
    }
}
