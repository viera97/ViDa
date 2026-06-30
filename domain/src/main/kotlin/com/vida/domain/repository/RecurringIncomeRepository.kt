package com.vida.domain.repository

import com.vida.domain.model.RecurringIncome
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Persistence contract for [RecurringIncome] templates. Implemented in `:data`
 * (Room).
 *
 * Query helpers:
 * - [getDue] returns templates whose next-due date is on or before [asOf] and
 *   whose [RecurringIncome.endDate] (if set) is on or after [asOf]. Filtering
 *   by `isActive = true` is the caller's responsibility (the use case filters
 *   the raw emission by template flag).
 */
interface RecurringIncomeRepository {
    fun getAll(): Flow<List<RecurringIncome>>
    suspend fun getById(id: Long): RecurringIncome?
    suspend fun getDue(asOf: LocalDate): Flow<List<RecurringIncome>>
    suspend fun upsert(recurring: RecurringIncome): Long
    suspend fun delete(id: Long)
}
