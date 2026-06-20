package com.vida.domain.usecase.recurring

import com.vida.domain.model.RecurringExpense
import com.vida.domain.repository.RecurringExpenseRepository

/** Loads a single recurring-expense template by id; returns null if not found. */
class GetRecurringExpense(private val repo: RecurringExpenseRepository) {
    suspend operator fun invoke(id: Long): RecurringExpense? {
        require(id > 0L) { "RecurringExpense id must be > 0" }
        return repo.getById(id)
    }
}
