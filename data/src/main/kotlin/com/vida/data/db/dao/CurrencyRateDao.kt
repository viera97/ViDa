package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vida.data.db.entity.CurrencyRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyRateDao {
    @Query("SELECT * FROM currency_rates ORDER BY effective_date DESC")
    fun observeAll(): Flow<List<CurrencyRateEntity>>

    /**
     * Latest rate for the [from]→[to] pair on or before [asOf], or null when none exists.
     * The composite index `idx_currency_rates_lookup` covers this hot path.
     */
    @Query(
        """
        SELECT * FROM currency_rates
        WHERE from_currency = :from AND to_currency = :to AND effective_date <= :asOf
        ORDER BY effective_date DESC LIMIT 1
        """,
    )
    suspend fun getRate(from: String, to: String, asOf: Long): CurrencyRateEntity?

    @Query(
        """
        SELECT * FROM currency_rates
        WHERE from_currency = :from AND to_currency = :to
        ORDER BY effective_date DESC
        """,
    )
    fun observeRateHistory(from: String, to: String): Flow<List<CurrencyRateEntity>>

    @Upsert
    suspend fun upsert(entity: CurrencyRateEntity): Long

    @Query("DELETE FROM currency_rates WHERE id = :id")
    suspend fun delete(id: Long)
}
