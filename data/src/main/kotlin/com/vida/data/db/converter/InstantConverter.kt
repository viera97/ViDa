package com.vida.data.db.converter

import androidx.room.TypeConverter
import java.time.Instant

/**
 * TypeConverter for [Instant] ↔ Long (epoch millis) serialization.
 *
 * Null-safe: maps null ↔ null.
 */
object InstantConverter {

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}
