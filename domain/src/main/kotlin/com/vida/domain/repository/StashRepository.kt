package com.vida.domain.repository

import com.vida.domain.model.Money
import com.vida.domain.model.Stash
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for [Stash] aggregates. Implemented in `:data` (Room).
 *
 * Both [getAll] and [observeBalance] are reactive — Room's Flow invalidation
 * ensures that consumers (e.g. [com.vida.domain.usecase.stash.ListStashes] and
 * [com.vida.domain.usecase.stash.GetStashBalance]) re-emit whenever the
 * underlying tables change, including after a transfer or expense.
 */
interface StashRepository {
    fun getAll(): Flow<List<Stash>>
    suspend fun getById(id: Long): Stash?
    suspend fun upsert(stash: Stash): Long
    suspend fun delete(id: Long)
    fun observeBalance(id: Long): Flow<Money>
}