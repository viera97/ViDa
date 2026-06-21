package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vida.data.db.entity.StashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StashDao {
    @Query("SELECT * FROM stashes ORDER BY name ASC")
    fun observeAll(): Flow<List<StashEntity>>

    @Query("SELECT * FROM stashes WHERE id = :id")
    suspend fun getById(id: Long): StashEntity?

    @Upsert
    suspend fun upsert(entity: StashEntity): Long

    @Query("DELETE FROM stashes WHERE id = :id")
    suspend fun delete(id: Long)
}
