package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vida.data.db.entity.BankEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    @Query("SELECT * FROM banks ORDER BY name ASC")
    fun observeAll(): Flow<List<BankEntity>>

    @Query("SELECT * FROM banks WHERE id = :id")
    suspend fun getById(id: Long): BankEntity?

    @Query("SELECT * FROM banks WHERE name = :name")
    suspend fun getByName(name: String): BankEntity?

    @Upsert
    suspend fun upsert(bank: BankEntity): Long

    @Query("DELETE FROM banks WHERE id = :id")
    suspend fun delete(id: Long)
}
