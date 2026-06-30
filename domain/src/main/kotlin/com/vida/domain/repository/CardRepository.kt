package com.vida.domain.repository

import com.vida.domain.model.Card
import com.vida.domain.model.Money
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for [Card] aggregates. Implemented in `:data` (Room).
 *
 * The reactive [getAll] and [observeBalance] emit on every underlying table change;
 * consumers (UI ViewModels via [com.vida.domain.usecase.card.ListCards] and
 * [com.vida.domain.usecase.card.GetCardBalance]) collect them as state so the UI
 * stays in sync after transfers, expenses, or mutations made elsewhere in the app.
 */
interface CardRepository {
    fun getAll(): Flow<List<Card>>
    suspend fun getById(id: Long): Card?
    suspend fun upsert(card: Card): Long
    suspend fun delete(id: Long)
    fun observeBalance(id: Long): Flow<Money>
}