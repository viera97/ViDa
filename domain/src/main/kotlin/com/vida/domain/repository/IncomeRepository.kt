package com.vida.domain.repository

import com.vida.domain.model.Income
import com.vida.domain.model.IncomeFilter
import com.vida.domain.model.SourceType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Persistence contract for [Income] aggregates. Implemented in `:data` (Room).
 *
 * Query helpers:
 * - [getBySource] returns incomes whose [sourceType]/[sourceId] match; `sourceId == null`
 *   selects wallet incomes.
 * - [getByDateRange] returns incomes with `dateTime` in `[from, to)`.
 * - [searchIncomes] returns a paginated slice matching optional [IncomeFilter] criteria,
 *   newest first. Mirrors [ExpenseRepository.searchExpenses].
 *
 * Recording ([upsert]) has a ledger effect on the destination source's
 * `balance_minor` — see [Income] docstring. Atomicity is a `:data` concern —
 * the implementation wraps the row insert + balance delta in a single Room
 * transaction so the two cannot drift.
 */
interface IncomeRepository {
    fun getAll(): Flow<List<Income>>
    suspend fun getById(id: Long): Income?
    suspend fun getBySource(sourceType: SourceType, sourceId: Long?, asOf: Instant): Flow<List<Income>>
    suspend fun getByDateRange(from: Instant, to: Instant): Flow<List<Income>>
    suspend fun searchIncomes(filter: IncomeFilter, limit: Int, offset: Int): List<Income>
    suspend fun upsert(income: Income): Long
    suspend fun delete(id: Long)
}
