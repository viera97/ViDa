package com.vida.domain.usecase.expense

import com.vida.domain.model.Expense
import com.vida.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

/** Streams the full expense list. Reactive: emits on every Room table change. */
class ListExpenses(private val repo: ExpenseRepository) {
    operator fun invoke(): Flow<List<Expense>> = repo.getAll()
}