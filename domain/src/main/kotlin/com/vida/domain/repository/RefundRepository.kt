package com.vida.domain.repository

import com.vida.domain.model.Refund
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for [Refund] aggregates. Implemented in `:data` (Room).
 *
 * The Room layer enforces UNIQUE on `originalExpenseId` (Q9 locked) and throws
 * `IllegalStateException` if a second refund tries to attach to the same expense.
 */
interface RefundRepository {
    fun getAll(): Flow<List<Refund>>
    suspend fun getById(id: Long): Refund?
    suspend fun getByOriginalExpense(expenseId: Long): Flow<List<Refund>>
    suspend fun upsert(refund: Refund): Long
    suspend fun delete(id: Long)
}