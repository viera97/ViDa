package com.vida.domain.repository

import com.vida.domain.model.CurrencyRate
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Persistence contract for [CurrencyRate] snapshots. Implemented in `:data` (Room).
 *
 * - [getRate] returns the latest rate for the pair where `updatedAt <= asOf`, or null.
 * - [getRateHistory] returns every snapshot for the pair, newest first.
 */
interface CurrencyRateRepository {
    fun getAll(): Flow<List<CurrencyRate>>
    suspend fun getRate(from: String, to: String, asOf: Instant): CurrencyRate?
    suspend fun getRateHistory(from: String, to: String): Flow<List<CurrencyRate>>
    suspend fun upsert(rate: CurrencyRate): Long
    suspend fun delete(id: Long)
}