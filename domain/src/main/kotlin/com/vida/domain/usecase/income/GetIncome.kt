package com.vida.domain.usecase.income

import com.vida.domain.model.Income
import com.vida.domain.repository.IncomeRepository

/** Loads a single income by its row id. Returns `null` when the id does not exist. */
class GetIncome(private val repo: IncomeRepository) {
    suspend operator fun invoke(id: Long): Income? {
        require(id > 0L) { "Income id must be > 0" }
        return repo.getById(id)
    }
}
