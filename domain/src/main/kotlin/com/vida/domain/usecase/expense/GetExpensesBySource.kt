package com.vida.domain.usecase.expense

import com.vida.domain.model.Expense
import com.vida.domain.model.SourceType
import com.vida.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Streams expenses that charged against the given source on/before [asOf]. */
class GetExpensesBySource(private val repo: ExpenseRepository) {
    suspend operator fun invoke(sourceType: SourceType, sourceId: Long?, asOf: Instant): Flow<List<Expense>> =
        repo.getBySource(sourceType, sourceId, asOf)
}