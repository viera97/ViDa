package com.vida.domain.usecase.recurring

import com.vida.domain.model.RecurringIncome
import com.vida.domain.repository.RecurringIncomeRepository
import kotlinx.coroutines.flow.Flow

/** Reactive stream of every recurring-income template in the system. */
class ListRecurringIncomes(private val repo: RecurringIncomeRepository) {
    operator fun invoke(): Flow<List<RecurringIncome>> = repo.getAll()
}
