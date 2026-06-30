package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vida.data.db.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY bank ASC")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): CardEntity?

    @Upsert
    suspend fun upsert(entity: CardEntity): Long

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Atomically applies [delta] (positive or negative, in minor units) to the stored
     * `balance_minor` column of the card row identified by [id]. Used by
     * `TransferOrchestrator` and `ExpenseRepositoryImpl` to keep the displayed balance
     * in sync with movement events (Option C — ledger semantics). Negative deltas
     * reduce the balance; positive deltas increase it. No-op if no row matches [id].
     */
    @Query("UPDATE cards SET balance_minor = balance_minor + :delta WHERE id = :id")
    suspend fun adjustBalance(id: Long, delta: Long)
}
