package com.vida.domain.model

/**
 * Supported currencies for ViDa.
 *
 * v1 ships three: CUP (Cuban peso), USD (US dollar), MLC (moneda libremente convertible).
 * Each entry carries its ISO-ish code and a human-readable symbol used by the UI.
 */
enum class Currency(val code: String, val symbol: String) {
    CUP("CUP", "$"),
    USD("USD", "$"),
    MLC("MLC", "$"),
    EUR("EUR", "€");

    companion object {
        private val DEFAULT = CUP

        /**
         * Parse a currency from its string [code]. Case-insensitive.
         *
         * Returns [DEFAULT] (CUP) when [code] does not match any known currency,
         * instead of throwing. This allows dynamic/user-created currencies to
         * fall back gracefully rather than crashing the app.
         */
        fun fromCode(code: String): Currency =
            values().firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: DEFAULT
    }
}