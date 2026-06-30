package com.vida.domain.usecase.recurring

import com.vida.domain.model.RecurringIncome
import com.vida.domain.repository.RecurringIncomeRepository

/** Updates an existing recurring-income template. The id MUST be > 0. */
class UpdateRecurringIncome(private val repo: RecurringIncomeRepository) {
    suspend operator fun invoke(recurring: RecurringIncome): Long {
        require(recurring.id > 0L) {
            "RecurringIncome id must be > 0 to update (use AddRecurringIncome for new templates)"
        }
        return repo.upsert(recurring)
    }
}
