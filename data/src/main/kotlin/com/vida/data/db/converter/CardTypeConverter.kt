package com.vida.data.db.converter

import androidx.room.TypeConverter
import com.vida.domain.model.CardType

/**
 * TypeConverter for [CardType] ↔ String (enum name) serialization.
 */
object CardTypeConverter {

    @TypeConverter
    fun fromCardType(value: CardType): String = value.name

    @TypeConverter
    fun toCardType(value: String): CardType = CardType.valueOf(value)
}
