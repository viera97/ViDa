package com.vida.domain.usecase.recurring

import com.vida.domain.model.RecurringExpense
import com.vida.domain.repository.RecurringExpenseRepository

/**
 * Persists a new recurring-expense template. The entity's invariants (positive
 * amount, non-blank description, valid date range, source nullness) are
 * enforced in [RecurringExpense.init] before this body runs.
 *
 * Per the spec's v1 manual-generation model (Q10 locked): this use case does
 * NOT trigger any generation. It only persists the template. Generation is
 * the caller's responsibility, via [GenerateRecurringExpense] (typically
 * invoked from a "due today" banner or app-launch hook in a `:feature-*`
 * change).
 */
class AddRecurringExpense(private val repo: RecurringExpenseRepository) {
    suspend operator fun invoke(recurring: RecurringExpense): Long {
        require(recurring.amount.isPositive()) { "RecurringExpense amount must be positive" }
        require(recurring.description.isNotBlank()) { "RecurringExpense description must not be blank" }
        // Templates land with no prior generation and active by default.
        val fresh = recurring.copy(lastGeneratedDate = null, isActive = true)
        return repo.upsert(fresh)
    }
}
