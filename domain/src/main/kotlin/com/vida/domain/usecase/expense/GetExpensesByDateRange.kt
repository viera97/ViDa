package com.vida.domain.usecase.expense

import com.vida.domain.model.Expense
import com.vida.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Streams expenses with `dateTime` in `[from, to)`. */
class GetExpensesByDateRange(private val repo: ExpenseRepository) {
    suspend operator fun invoke(from: Instant, to: Instant): Flow<List<Expense>> =
        repo.getByDateRange(from, to)
}