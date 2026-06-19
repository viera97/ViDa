package com.vida.domain.repository

import com.vida.domain.model.Money
import com.vida.domain.model.Stash
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Persistence contract for [Stash] aggregates. Implemented in `:data` (Room). */
interface StashRepository {
    fun getAll(): Flow<List<Stash>>
    suspend fun getById(id: Long): Stash?
    suspend fun upsert(stash: Stash): Long
    suspend fun delete(id: Long)
    suspend fun getBalance(id: Long, asOf: Instant = Instant.now()): Money
}