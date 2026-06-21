package com.vida.data.db.converter

import androidx.room.TypeConverter
import com.vida.domain.model.SourceType

/**
 * TypeConverter for [SourceType] ↔ String (enum name) serialization.
 */
object SourceTypeConverter {

    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = SourceType.valueOf(value)
}
