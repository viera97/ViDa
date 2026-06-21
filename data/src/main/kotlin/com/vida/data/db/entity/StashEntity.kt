package com.vida.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vida.domain.model.Currency
import java.time.Instant

@Entity(tableName = "stashes")
data class StashEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val currency: Currency,
)
