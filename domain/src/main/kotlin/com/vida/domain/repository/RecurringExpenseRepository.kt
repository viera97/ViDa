package com.vida.domain.repository

import com.vida.domain.model.RecurringExpense
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Persistence contract for [RecurringExpense] templates. Implemented in `:data`
 * (Room).
 *
 * Query helpers:
 * - [getDue] returns templates whose next-due date is on or before [asOf] and
 *   whose [RecurringExpense.endDate] (if set) is on or after [asOf]. Filtering
 *   by `isActive = true` is the caller's responsibility (the use case filters
 *   the raw emission by template flag).
 */
interface RecurringExpenseRepository {
    fun getAll(): Flow<List<RecurringExpense>>
    suspend fun getById(id: Long): RecurringExpense?
    suspend fun getDue(asOf: LocalDate): Flow<List<RecurringExpense>>
    suspend fun upsert(recurring: RecurringExpense): Long
    suspend fun delete(id: Long)
}
