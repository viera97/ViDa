package com.vida.domain.usecase.recurring

import com.vida.domain.model.Expense
import com.vida.domain.model.RecurringExpense
import com.vida.domain.repository.RecurringExpenseRepository
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Action use case for manually generating an occurrence from a recurring-expense
 * template. v1 is **manual** — there is no WorkManager, no scheduled job, no
 * alarm (Q10 locked). The UI triggers this from a "due today" banner or an
 * app-launch hook in a `:feature-*` change.
 *
 ## v1 contract (deliberate simplification)

 This use case does **NOT** persist the generated [Expense]. It only:

 1. Loads the [RecurringExpense] template via [RecurringExpenseRepository.getById].
 2. Computes the new [Expense] (in memory only — for inspection / future use).
 3. Updates the template's `lastGeneratedDate = asOf` via
    [RecurringExpenseRepository.upsert].

 The new [Expense] row is created by the **caller** via [com.vida.domain.usecase.expense.RecordExpense].
 The use case returns `0L` (the "unsaved" sentinel for the row id) because no
 persistence happens here.

 This split keeps `GenerateRecurringExpense` single-responsibility (template
 bookkeeping) and lets the caller decide where the new Expense lands in the
 transaction (today, in the future, or batched with other operations).

 When the `:data` change wires `RecordExpense` into a Hilt-managed path, a
 future change can promote this use case to a "full" generator that creates
 and persists the Expense in one `withTransaction { }`. The current shape is
 intentionally minimal so the `RecordExpense` (PR #2b) gets first-class
 validation (source exists, category exists) and so the test surface stays
 pure-MockK without a Room in-memory DB.

 @param recurringId the template row id
 @param asOf the local date the occurrence is attributed to; default today
 @return `0L` (sentinel — no Expense row was persisted by this call)
 @throws NoSuchElementException when no template exists with [recurringId]
 */
class GenerateRecurringExpense(private val repo: RecurringExpenseRepository) {
    suspend operator fun invoke(
        recurringId: Long,
        asOf: LocalDate = LocalDate.now(),
    ): Long {
        val template = repo.getById(recurringId)
            ?: throw NoSuchElementException("Recurring expense $recurringId not found")

        // Constructed for caller inspection / future expansion. NOT persisted here —
        // the caller persists via `RecordExpense`, which performs source/category
        // existence checks and other cross-entity validation that belongs to the
        // "record an expense" use case, not to "advance a template".
        @Suppress("UNUSED_VARIABLE")
        val newExpense = Expense(
            id = 0L,
            categoryId = template.categoryId,
            amount = template.amount,
            realAmount = null,
            description = template.description,
            dateTime = asOf.atStartOfDay(ZoneOffset.UTC).toInstant(),
            sourceType = template.sourceType,
            sourceId = template.sourceId,
            note = null,
        )

        val updatedTemplate: RecurringExpense = template.copy(lastGeneratedDate = asOf)
        repo.upsert(updatedTemplate)

        // Sentinel: no row was inserted here.
        return 0L
    }
}
