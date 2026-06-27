package com.vida.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vida.domain.model.Currency

/**
 * Room entity for the `currency_rates` table.
 *
 * [fromCurrency]/[toCurrency] use the [Currency] TypeConverter (stored as TEXT currency code).
 * [rate] is REAL (Double) — the mapper converts to/from domain BigDecimal. [effectiveDate]
 * is epoch millis. The composite index covers the `getRate` "latest on or before cutoff"
 * hot path.
 */
@Entity(
    tableName = "currency_rates",
    indices = [
        Index(value = ["from_currency", "to_currency", "effective_date"], name = "idx_currency_rates_lookup"),
    ],
)
data class CurrencyRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "from_currency") val fromCurrency: Currency,
    @ColumnInfo(name = "to_currency") val toCurrency: Currency,
    val rate: Double,
    @ColumnInfo(name = "effective_date") val effectiveDate: Long,
    @ColumnInfo(name = "provider", defaultValue = "Manual") val provider: String = "Manual",
)
