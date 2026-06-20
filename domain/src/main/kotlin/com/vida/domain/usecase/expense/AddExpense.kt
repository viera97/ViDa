package com.vida.domain.usecase.expense

import com.vida.domain.model.Expense
import com.vida.domain.repository.ExpenseRepository

/**
 * Persists a new expense. The entity's invariants (positive amount, non-blank
 * description, realAmount currency, source/sourceId pairing) are enforced in
 * [Expense.init]; this use case repeats the cheap ones defensively.
 *
 * Cross-entity validation (source exists, category exists) lives in PR #2b's
 * `RecordExpense` — this is the low-level CRUD primitive.
 */
class AddExpense(private val repo: ExpenseRepository) {
    suspend operator fun invoke(expense: Expense): Long {
        require(expense.amount.isPositive()) { "Expense amount must be positive" }
        require(expense.description.isNotBlank()) { "Expense description must not be blank" }
        return repo.upsert(expense)
    }
}