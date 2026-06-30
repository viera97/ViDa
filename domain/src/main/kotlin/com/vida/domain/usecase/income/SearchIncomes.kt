package com.vida.domain.usecase.income

import com.vida.domain.model.Income
import com.vida.domain.model.IncomeFilter
import com.vida.domain.repository.IncomeRepository

/**
 * Searches incomes with optional filter criteria and manual offset pagination.
 * Mirrors [com.vida.domain.usecase.expense.SearchExpenses] but for incomes.
 */
class SearchIncomes(private val repo: IncomeRepository) {
    suspend operator fun invoke(
        filter: IncomeFilter,
        limit: Int,
        offset: Int,
    ): List<Income> = repo.searchIncomes(filter, limit, offset)
}
