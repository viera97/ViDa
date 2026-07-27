package com.vida.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `currencies` table.
 *
 * [isSystem] is stored as INTEGER (0/1); the mapper converts to/from the domain
 * [com.vida.domain.model.CurrencyInfo] boolean.
 *
 * The unique index on [code] enforces ISO 4217 code uniqueness; the equivalent
 * migration is `MIGRATION_12_13` for users upgrading from a pre-v13 schema.
 */
@Entity(
    tableName = "currencies",
    indices = [Index(value = ["code"], unique = true)],
)
data class CurrencyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val code: String,
    @ColumnInfo(name = "is_system") val isSystem: Int,
)
