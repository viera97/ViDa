package com.vida.domain.repository

import com.vida.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for [Category] aggregates. Implemented in `:data` (Room).
 *
 * Reactive [getAll] emits on every underlying table change; consumers
 * (`ListCategories`) collect it as state.
 */
interface CategoryRepository {
    fun getAll(): Flow<List<Category>>
    suspend fun getById(id: Long): Category?
    suspend fun upsert(category: Category): Long
    suspend fun delete(id: Long)
}