package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vida.data.db.entity.CurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies ORDER BY code ASC")
    fun observeAll(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE id = :id")
    suspend fun getById(id: Long): CurrencyEntity?

    @Query("SELECT * FROM currencies WHERE code = :code")
    suspend fun getByCode(code: String): CurrencyEntity?

    @Upsert
    suspend fun upsert(currency: CurrencyEntity): Long

    @Query("DELETE FROM currencies WHERE id = :id")
    suspend fun delete(id: Long)
}
