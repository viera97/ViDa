package com.vida.domain.model

/**
 * Supported currencies for ViDa.
 *
 * v1 ships three: CUP (Cuban peso), USD (US dollar), MLC (moneda libremente convertible).
 * Each entry carries its ISO-ish code and a human-readable symbol used by the UI.
 */
enum class Currency(val code: String, val symbol: String) {
    CUP("CUP", "$"),
    USD("USD", "USD"),
    MLC("MLC", "MLC");

    companion object {
        /**
         * Parse a currency from its string [code]. Case-insensitive.
         *
         * @throws IllegalArgumentException when [code] does not match any known currency.
         */
        fun fromCode(code: String): Currency =
            values().firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown currency code: $code")
    }
}