package com.vida.data.db.converter

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * TypeConverter for [LocalDate] ↔ Long (epoch day) serialization.
 *
 * Null-safe: maps null ↔ null.
 */
object LocalDateConverter {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }
}
