package com.vida.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the `categories` table.
 *
 * [isSystem] is stored as INTEGER (0/1); the mapper converts to/from the domain
 * [com.vida.domain.model.Category] boolean.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val color: Int,
    val icon: String?,
    @ColumnInfo(name = "is_system") val isSystem: Int,
)
