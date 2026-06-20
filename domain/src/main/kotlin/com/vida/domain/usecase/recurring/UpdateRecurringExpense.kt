package com.vida.domain.usecase.recurring

import com.vida.domain.model.RecurringExpense
import com.vida.domain.repository.RecurringExpenseRepository

/** Updates an existing recurring-expense template. The id MUST be > 0. */
class UpdateRecurringExpense(private val repo: RecurringExpenseRepository) {
    suspend operator fun invoke(recurring: RecurringExpense): Long {
        require(recurring.id > 0L) {
            "RecurringExpense id must be > 0 to update (use AddRecurringExpense for new templates)"
        }
        return repo.upsert(recurring)
    }
}
