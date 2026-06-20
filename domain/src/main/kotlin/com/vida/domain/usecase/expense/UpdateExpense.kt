package com.vida.domain.usecase.expense

import com.vida.domain.model.Expense
import com.vida.domain.repository.ExpenseRepository

/** Updates an existing expense. The id MUST be > 0. */
class UpdateExpense(private val repo: ExpenseRepository) {
    suspend operator fun invoke(expense: Expense): Long {
        require(expense.id > 0L) { "Expense id must be > 0 to update" }
        return repo.upsert(expense)
    }
}