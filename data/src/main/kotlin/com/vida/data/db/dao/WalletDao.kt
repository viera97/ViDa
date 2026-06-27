package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vida.data.db.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY name ASC")
    fun observeAll(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getById(id: Long): WalletEntity?

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun delete(id: Long)

    @Upsert
    suspend fun upsert(entity: WalletEntity)

    /**
     * Atomically applies [delta] (positive or negative, in minor units) to the stored
     * `balance_minor` column of the wallet row identified by [id]. Used by
     * `TransferOrchestrator` and `ExpenseRepositoryImpl` to keep the displayed balance
     * in sync with movement events (Option C — ledger semantics). Negative deltas
     * reduce the balance; positive deltas increase it. No-op if no row matches [id].
     */
    @Query("UPDATE wallets SET balance_minor = balance_minor + :delta WHERE id = :id")
    suspend fun adjustBalance(id: Long, delta: Long)
}
