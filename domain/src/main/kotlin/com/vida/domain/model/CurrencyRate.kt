package com.vida.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * An exchange rate snapshot between two [Currency]s at a point in time.
 *
 * Invariants enforced in `init {}`:
 *
 * - `rate` MUST be strictly positive (signum > 0; zero and negative are rejected)
 * - `fromCurrency` MUST differ from `toCurrency` (identity conversion is free)
 *
 * One row per (fromCurrency, toCurrency, updatedAt) tuple (Q5 locked). The `:data`
 * Room layer uses (fromCurrency, toCurrency) as the natural key; multiple historical
 * snapshots are kept ordered by `updatedAt DESC`.
 *
 * @property id row id (0 means unsaved)
 * @property fromCurrency the "from" side of the pair
 * @property toCurrency the "to" side of the pair
 * @property rate multiplier such that `1 fromCurrency == rate toCurrency`
 * @property updatedAt when this snapshot was recorded (UTC)
 */
data class CurrencyRate(
    val id: Long = 0L,
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val rate: BigDecimal,
    val updatedAt: Instant,
) {
    init {
        require(rate.signum() > 0) { "CurrencyRate rate must be positive" }
        require(fromCurrency != toCurrency) { "CurrencyRate fromCurrency must differ from toCurrency" }
    }
}