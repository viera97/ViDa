package com.vida.domain.repository

import com.vida.domain.model.Expense
import com.vida.domain.model.SourceType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Persistence contract for [Expense] aggregates. Implemented in `:data` (Room).
 *
 * Query helpers:
 * - [getBySource] returns expenses whose [sourceType]/[sourceId] match; `sourceId == null`
 *   selects wallet expenses.
 * - [getByCategory] returns expenses in [categoryId] on/before [asOf].
 * - [getByDateRange] returns expenses with `dateTime` in `[from, to)`.
 */
interface ExpenseRepository {
    fun getAll(): Flow<List<Expense>>
    suspend fun getById(id: Long): Expense?
    suspend fun getBySource(sourceType: SourceType, sourceId: Long?, asOf: Instant): Flow<List<Expense>>
    suspend fun getByCategory(categoryId: Long, asOf: Instant): Flow<List<Expense>>
    suspend fun getByDateRange(from: Instant, to: Instant): Flow<List<Expense>>
    suspend fun upsert(expense: Expense): Long
    suspend fun delete(id: Long)
}