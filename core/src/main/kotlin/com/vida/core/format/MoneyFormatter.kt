package com.vida.core.format

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val cupLocaleFormat: NumberFormat by lazy {
    NumberFormat.getNumberInstance(Locale("es", "CU")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
}

/**
 * Formats [this] [Money] as a human-readable string.
 *
 * The ISO currency code (CUP / USD / MLC / EUR) is always appended after the
 * amount. Symbols ($ / €) are NOT used because ViDa's three of four supported
 * currencies share the `$` glyph (CUP, USD, MLC) — code disambiguates them
 * unambiguously, symbols don't.
 *
 * Examples:
 * - `Money(1250.50, CUP)` → "1,250.50 CUP"
 * - `Money(99.00, USD)`   → "99.00 USD"
 * - `Money(0, MLC)`       → "0.00 MLC"
 * - `Money(10.00, EUR)`   → "10.00 EUR"
 */
fun formatMoney(money: Money): String {
    val formattedAmount = cupLocaleFormat.format(money.amount)
    return "$formattedAmount ${money.currency.code}"
}

/**
 * Formats a [BigDecimal] rate as a short decimal string.
 * Examples: `150.0` → "150.00", `1.2345` → "1.23"
 */
fun formatRate(rate: BigDecimal): String {
    return cupLocaleFormat.format(rate)
}
