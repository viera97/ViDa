package com.vida.data.db.converter

import androidx.room.TypeConverter
import com.vida.domain.model.Frequency

/**
 * TypeConverter for [Frequency] ↔ String (enum name) serialization.
 *
 * Ships in PR #2 as a standalone, zero-dependency converter. It is also registered on
 * the aggregated [com.vida.data.db.Converters] class so Room can use it when
 * `RecurringExpenseEntity` arrives in PR #3.
 */
object FrequencyConverter {

    @TypeConverter
    fun fromFrequency(value: Frequency): String = value.name

    @TypeConverter
    fun toFrequency(value: String): Frequency = Frequency.valueOf(value)
}
