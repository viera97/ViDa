package com.vida.data.mapper.util

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import java.time.Instant

/**
 * Converts a [Money] value to its decomposed column representation for Room storage.
 *
 * @return Pair of (minorUnits, currencyCode) — the two columns written to each
 *         Room entity that holds a Money field.
 */
fun Money.toColumns(): Pair<Long, String> =
    amountMinorUnits() to currency.code

/**
 * Converts a pair of (minorUnits, currencyCode) back into a [Money] domain value.
 */
fun Pair<Long, String>.toMoney(): Money {
    val (minorUnits, currencyCode) = this
    val currency = Currency.fromCode(currencyCode)
    return Money.fromMinorUnits(minorUnits, currency)
}

/**
 * Extracts the minor-unit representation (2-decimal fixed-point) of this Money value.
 *
 * Uses HALF_EVEN rounding per the domain [Money] convention. The scale is shifted
 * by 2 (e.g., 12.34 CUP → 1234L; -5.00 USD → -500L).
 */
fun Money.amountMinorUnits(): Long =
    amount.multiply(java.math.BigDecimal(100))
        .setScale(0, java.math.RoundingMode.HALF_EVEN)
        .toLong()

/**
 * Companion-style factory to reconstruct [Money] from minor units.
 */
fun Money.Companion.fromMinorUnits(minorUnits: Long, currency: Currency): Money =
    Money(
        amount = java.math.BigDecimal(minorUnits).divide(java.math.BigDecimal(100), 2, java.math.RoundingMode.HALF_EVEN),
        currency = currency,
    )

/**
 * Converts an [Instant] to epoch millis for Room storage.
 */
fun Instant.toEpochMillis(): Long = this.toEpochMilli()

/**
 * Converts epoch millis back to an [Instant].
 */
fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)
