package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vida.data.db.entity.WalletEntity

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE id = 1")
    suspend fun get(): WalletEntity?

    @Upsert
    suspend fun upsert(entity: WalletEntity)
}
