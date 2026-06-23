package com.vida.domain.usecase.expense

import com.vida.domain.model.Expense
import com.vida.domain.repository.ExpenseRepository

/**
 * Loads a single expense by its row id. Returns `null` when the id does not exist.
 */
class GetExpense(private val repo: ExpenseRepository) {
    suspend operator fun invoke(id: Long): Expense? {
        require(id > 0L) { "Expense id must be > 0" }
        return repo.getById(id)
    }
}
