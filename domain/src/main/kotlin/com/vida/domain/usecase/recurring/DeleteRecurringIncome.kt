package com.vida.domain.usecase.recurring

import com.vida.domain.repository.RecurringIncomeRepository

/** Removes a recurring-income template by its row id. */
class DeleteRecurringIncome(private val repo: RecurringIncomeRepository) {
    suspend operator fun invoke(id: Long) {
        require(id > 0L) { "RecurringIncome id must be > 0" }
        repo.delete(id)
    }
}
