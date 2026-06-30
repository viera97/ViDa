package com.vida.domain.usecase.recurring

import com.vida.domain.model.RecurringIncome
import com.vida.domain.repository.RecurringIncomeRepository

/**
 * Persists a new recurring-income template. The entity's invariants (positive
 * amount, non-blank description, valid date range, source nullness) are
 * enforced in [RecurringIncome.init] before this body runs.
 *
 * Per the spec's v1 manual-generation model: this use case does
 * NOT trigger any generation. It only persists the template. Generation is
 * the caller's responsibility, via [GenerateRecurringIncome].
 */
class AddRecurringIncome(private val repo: RecurringIncomeRepository) {
    suspend operator fun invoke(recurring: RecurringIncome): Long {
        require(recurring.amount.isPositive()) { "RecurringIncome amount must be positive" }
        require(recurring.description.isNotBlank()) { "RecurringIncome description must not be blank" }
        // Templates land with no prior generation and active by default.
        val fresh = recurring.copy(lastGeneratedDate = null, isActive = true)
        return repo.upsert(fresh)
    }
}
