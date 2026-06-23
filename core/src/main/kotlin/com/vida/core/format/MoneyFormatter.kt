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
 * Currency symbol is prepended for CUP; for USD/MLC the currency code is appended.
 *
 * Examples:
 * - `Money(1250.50, CUP)` → "1,250.50 $"
 * - `Money(99.00, USD)`   → "99.00 USD"
 * - `Money(0, CUP)`       → "0.00 $"
 */
fun formatMoney(money: Money): String {
    val formattedAmount = cupLocaleFormat.format(money.amount)
    return when (money.currency) {
        Currency.CUP -> "$formattedAmount ${money.currency.symbol}"
        Currency.USD, Currency.MLC -> "$formattedAmount ${money.currency.code}"
    }
}

/**
 * Formats a [BigDecimal] rate as a short decimal string.
 * Examples: `150.0` → "150.00", `1.2345` → "1.23"
 */
fun formatRate(rate: BigDecimal): String {
    return cupLocaleFormat.format(rate)
}
