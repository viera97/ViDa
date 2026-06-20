package com.vida.domain.usecase.recurring

import com.vida.domain.model.RecurringExpense
import com.vida.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow

/** Reactive stream of every recurring-expense template in the system. */
class ListRecurringExpenses(private val repo: RecurringExpenseRepository) {
    operator fun invoke(): Flow<List<RecurringExpense>> = repo.getAll()
}
