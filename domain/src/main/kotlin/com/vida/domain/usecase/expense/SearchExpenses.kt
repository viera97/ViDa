package com.vida.domain.usecase.expense

import com.vida.domain.model.Expense
import com.vida.domain.model.ExpenseFilter
import com.vida.domain.repository.ExpenseRepository

/**
 * Searches expenses with optional filter criteria and manual offset pagination.
 *
 * Delegates to [ExpenseRepository.searchExpenses]. All filter fields are optional;
 * an all-null filter returns the full expense list (limited by [limit]/[offset]).
 */
class SearchExpenses(private val repo: ExpenseRepository) {
    suspend operator fun invoke(
        filter: ExpenseFilter,
        limit: Int,
        offset: Int,
    ): List<Expense> = repo.searchExpenses(filter, limit, offset)
}
