package com.vida.feature.home.home

import com.vida.core.format.formatMoney
import com.vida.domain.model.Money

/**
 * Formats [this] [Money] for the home dashboard.
 *
 * Kept as a thin facade over [com.vida.core.format.formatMoney] now that the
 * core formatter also renders ISO currency codes (no `$` / `€` symbols).
 * Call sites in feature-home can keep importing this for readability —
 * "formatHomeMoney" reads as "format money the way the home dashboard wants
 * it" even though today that equals the global default.
 *
 * Examples:
 * - `Money(1250.50, CUP)` → "1,250.50 CUP"
 * - `Money(99.00, USD)`   → "99.00 USD"
 * - `Money(0, MLC)`       → "0.00 MLC"
 * - `Money(10, EUR)`      → "10.00 EUR"
 */
fun formatHomeMoney(money: Money): String = formatMoney(money)
