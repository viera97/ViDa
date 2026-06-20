package com.vida.domain.usecase.expense

import com.vida.domain.repository.ExpenseRepository

/**
 * Deletes an expense by id. Refunds tied to the deleted expense SURVIVE as
 * historical records (Q9 locked); the UI is responsible for surfacing the
 * "orphan refund" state if it matters to the user.
 */
class DeleteExpense(private val repo: ExpenseRepository) {
    suspend operator fun invoke(id: Long) {
        require(id > 0L) { "Expense id must be > 0" }
        repo.delete(id)
    }
}