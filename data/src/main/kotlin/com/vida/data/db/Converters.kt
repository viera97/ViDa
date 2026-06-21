package com.vida.data.db

import androidx.room.TypeConverter
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.model.SourceType
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromCurrency(value: Currency): String = value.code

    @TypeConverter
    fun toCurrency(value: String): Currency = Currency.fromCode(value)

    @TypeConverter
    fun fromInstant(value: Instant): Long = value.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @TypeConverter
    fun fromLocalDate(value: LocalDate): Long = value.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun fromCardType(value: CardType): String = value.name

    @TypeConverter
    fun toCardType(value: String): CardType = CardType.valueOf(value)

    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = SourceType.valueOf(value)
}
