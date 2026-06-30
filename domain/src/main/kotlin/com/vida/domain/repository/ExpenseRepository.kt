package com.vida.domain.repository

import com.vida.domain.model.Expense
import com.vida.domain.model.ExpenseFilter
import com.vida.domain.model.SourceType
import com.vida.domain.model.aggregate.CategoryExpenseTotal
import com.vida.domain.model.aggregate.CurrencyTotal
import com.vida.domain.model.aggregate.PeriodCategoryExpenseTotal
import com.vida.domain.model.aggregate.PeriodExpenseTotal
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
 * - [searchExpenses] returns a paginated slice matching optional [filter] criteria
 *   (newest first).
 */
interface ExpenseRepository {
    fun getAll(): Flow<List<Expense>>
    suspend fun getById(id: Long): Expense?
    suspend fun getBySource(sourceType: SourceType, sourceId: Long?, asOf: Instant): Flow<List<Expense>>
    suspend fun getByCategory(categoryId: Long, asOf: Instant): Flow<List<Expense>>
    suspend fun getByDateRange(from: Instant, to: Instant): Flow<List<Expense>>
    suspend fun searchExpenses(filter: ExpenseFilter, limit: Int, offset: Int): List<Expense>
    // ── Aggregation methods for statistics ─────────────────────────────────
    suspend fun getExpenseTotalsByCategory(from: Instant, to: Instant): List<CategoryExpenseTotal>
    suspend fun getExpenseTotalsByPeriod(from: Instant, to: Instant, bucketMillis: Long): List<PeriodExpenseTotal>
    suspend fun getExpenseCategoryTotalsByPeriod(from: Instant, to: Instant, bucketMillis: Long): List<PeriodCategoryExpenseTotal>
    suspend fun getExpenseTotalsByCurrency(from: Instant, to: Instant): List<CurrencyTotal>

    suspend fun upsert(expense: Expense): Long
    suspend fun delete(id: Long)
}