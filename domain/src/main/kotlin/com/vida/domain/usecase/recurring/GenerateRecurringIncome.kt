package com.vida.domain.usecase.recurring

import com.vida.domain.model.Income
import com.vida.domain.model.RecurringIncome
import com.vida.domain.repository.RecurringIncomeRepository
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Action use case for manually generating an occurrence from a recurring-income
 * template. v1 is **manual** — there is no WorkManager, no scheduled job, no
 * alarm.
 *
 * ## v1 contract (deliberate simplification)
 *
 * This use case does **NOT** persist the generated [Income]. It only:
 *
 *  1. Loads the [RecurringIncome] template via [RecurringIncomeRepository.getById].
 *  2. Computes the new [Income] (in memory only — for inspection / future use).
 *  3. Updates the template's `lastGeneratedDate = asOf` via
 *     [RecurringIncomeRepository.upsert].
 *
 * The new [Income] row is created by the **caller** via [com.vida.domain.usecase.income.RecordIncome].
 * The use case returns `0L` (the "unsaved" sentinel for the row id) because no
 * persistence happens here.
 *
 * This split keeps `GenerateRecurringIncome` single-responsibility (template
 * bookkeeping) and lets the caller decide where the new Income lands in the
 * transaction (today, in the future, or batched with other operations).
 *
 * @param recurringId the template row id
 * @param asOf the local date the occurrence is attributed to; default today
 * @return `0L` (sentinel — no Income row was persisted by this call)
 * @throws NoSuchElementException when no template exists with [recurringId]
 */
class GenerateRecurringIncome(private val repo: RecurringIncomeRepository) {
    suspend operator fun invoke(
        recurringId: Long,
        asOf: LocalDate = LocalDate.now(),
    ): Long {
        val template = repo.getById(recurringId)
            ?: throw NoSuchElementException("Recurring income $recurringId not found")

        // Constructed for caller inspection / future expansion. NOT persisted here —
        // the caller persists via `RecordIncome`, which performs source existence
        // checks and other cross-entity validation.
        @Suppress("UNUSED_VARIABLE")
        val newIncome = Income(
            id = 0L,
            amount = template.amount,
            description = template.description,
            dateTime = asOf.atStartOfDay(ZoneOffset.UTC).toInstant(),
            sourceType = template.sourceType,
            sourceId = template.sourceId,
            note = null,
        )

        val updatedTemplate: RecurringIncome = template.copy(lastGeneratedDate = asOf)
        repo.upsert(updatedTemplate)

        // Sentinel: no row was inserted here.
        return 0L
    }
}
