package com.vida.domain.usecase.income

import com.vida.domain.model.Income
import com.vida.domain.repository.IncomeRepository

/** Updates an existing income. The id MUST be > 0. */
class UpdateIncome(private val repo: IncomeRepository) {
    suspend operator fun invoke(income: Income): Long {
        require(income.id > 0L) { "Income id must be > 0 to update" }
        return repo.upsert(income)
    }
}
