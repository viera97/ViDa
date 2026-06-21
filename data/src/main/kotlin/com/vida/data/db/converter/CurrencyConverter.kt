package com.vida.data.db.converter

import androidx.room.TypeConverter
import com.vida.domain.model.Currency

/**
 * TypeConverter for [Currency] ↔ String (currency code) serialization.
 */
object CurrencyConverter {

    @TypeConverter
    fun fromCurrency(value: Currency): String = value.code

    @TypeConverter
    fun toCurrency(value: String): Currency = Currency.fromCode(value)
}
