package com.vida.domain.usecase.recurring

import com.vida.domain.model.Frequency
import com.vida.domain.model.RecurringExpense
import com.vida.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Reactive stream of templates whose **next due date is on or before [asOf]**
 * and which are still within their optional `endDate` window. The underlying
 * source is `repo.getAll()`, re-filtered on every emission so a UI banner can
 * react when the user toggles a template's `isActive` flag mid-session.
 *
 * Date math (per spec REQ-2b-12, design §10):
 *
 * - `nextDueDate` = `lastGeneratedDate + frequency unit` when the template has
 *   been generated before; otherwise it falls back to `startDate`.
 * - Frequency units are `DAILY = 1 day`, `WEEKLY = 7 days`,
 *   `MONTHLY = Period.ofMonths(1)`, `YEARLY = Period.ofYears(1)`. The use of
 *   `Period` (rather than `plusDays(30)`) preserves Java's month-end clamp:
 *   `2026-01-31 + 1 month == 2026-02-28`, `2024-02-29 + 1 year == 2025-02-28`.
 * - A template whose `nextDueDate` falls after its `endDate` (or whose `asOf`
 *   has passed `endDate`) is treated as expired and excluded.
 *
 * The check is intentionally pure: no side effects, no DB writes.
 */
class GetDueRecurringExpenses(private val repo: RecurringExpenseRepository) {
    operator fun invoke(asOf: LocalDate = LocalDate.now()): Flow<List<RecurringExpense>> =
        repo.getAll().map { all ->
            all.filter { r -> r.isActive && isDue(r, asOf) }
        }

    private fun isDue(r: RecurringExpense, asOf: LocalDate): Boolean {
        val candidate = nextDueDate(r) ?: return false
        if (r.endDate != null && asOf.isAfter(r.endDate)) return false
        return !candidate.isAfter(asOf)
    }

    private fun nextDueDate(r: RecurringExpense): LocalDate? {
        val lastGen = r.lastGeneratedDate
        val candidate = if (lastGen != null) {
            when (r.frequency) {
                Frequency.DAILY -> lastGen.plusDays(1)
                Frequency.WEEKLY -> lastGen.plusWeeks(1)
                Frequency.MONTHLY -> lastGen.plusMonths(1)
                Frequency.YEARLY -> lastGen.plusYears(1)
            }
        } else {
            r.startDate
        }
        // startDate not yet reached — treat as "not eligible yet".
        if (candidate.isBefore(r.startDate)) return null
        return candidate
    }
}
