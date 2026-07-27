package com.vida.domain.repository

import com.vida.domain.model.CurrencyInfo
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for [CurrencyInfo] aggregates. Implemented in `:data` (Room).
 *
 * Reactive [getAll] emits on every underlying table change; consumers
 * (`ListCurrencies`) collect it as state.
 */
interface CurrencyRepository {
    fun getAll(): Flow<List<CurrencyInfo>>
    suspend fun getById(id: Long): CurrencyInfo?
    suspend fun getByCode(code: String): CurrencyInfo?
    suspend fun upsert(currency: CurrencyInfo): Long
    suspend fun delete(id: Long)
}
