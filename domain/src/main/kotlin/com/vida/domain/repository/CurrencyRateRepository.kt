package com.vida.domain.repository

import com.vida.domain.model.Currency
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
    suspend fun getRate(from: Currency, to: Currency, asOf: Instant): CurrencyRate?
    suspend fun getRateHistory(from: Currency, to: Currency): Flow<List<CurrencyRate>>
    suspend fun upsert(rate: CurrencyRate): Long
    suspend fun delete(id: Long)
}