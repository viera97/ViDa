package com.vida.domain.usecase.recurring

import com.vida.domain.repository.RecurringExpenseRepository

/** Removes a recurring-expense template by its row id. */
class DeleteRecurringExpense(private val repo: RecurringExpenseRepository) {
    suspend operator fun invoke(id: Long) {
        require(id > 0L) { "RecurringExpense id must be > 0" }
        repo.delete(id)
    }
}
