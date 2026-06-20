package com.vida.domain.usecase.expense

import com.vida.domain.model.Expense
import com.vida.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Streams expenses in [categoryId] on/before [asOf]. */
class GetExpensesByCategory(private val repo: ExpenseRepository) {
    suspend operator fun invoke(categoryId: Long, asOf: Instant): Flow<List<Expense>> =
        repo.getByCategory(categoryId, asOf)
}