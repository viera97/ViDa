package com.vida.domain.usecase.recurring

import com.vida.domain.model.RecurringIncome
import com.vida.domain.repository.RecurringIncomeRepository

/** Loads a single recurring-income template by id; returns null if not found. */
class GetRecurringIncome(private val repo: RecurringIncomeRepository) {
    suspend operator fun invoke(id: Long): RecurringIncome? {
        require(id > 0L) { "RecurringIncome id must be > 0" }
        return repo.getById(id)
    }
}
