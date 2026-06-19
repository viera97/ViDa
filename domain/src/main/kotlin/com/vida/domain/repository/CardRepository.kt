package com.vida.domain.repository

import com.vida.domain.model.Card
import com.vida.domain.model.Money
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Persistence contract for [Card] aggregates. Implemented in `:data` (Room).
 *
 * The reactive [getAll] emits on every underlying table change; consumers (UI ViewModels
 * via [com.vida.domain.usecase.card.ListCards]) collect it as state.
 */
interface CardRepository {
    fun getAll(): Flow<List<Card>>
    suspend fun getById(id: Long): Card?
    suspend fun upsert(card: Card): Long
    suspend fun delete(id: Long)
    suspend fun getBalance(id: Long, asOf: Instant = Instant.now()): Money
}