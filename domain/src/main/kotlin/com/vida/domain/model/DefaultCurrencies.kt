package com.vida.domain.model

/**
 * Seed set of system currencies inserted on first run via
 * `SeedDefaultCurrencies`. The id is the placeholder `0L`; the persistence layer
 * assigns the real row id on insert.
 *
 * Names are kept in Spanish to match the app UI.
 */
object DefaultCurrencies {
    val CUP: CurrencyInfo = CurrencyInfo(
        id = 0L,
        name = "Peso cubano",
        code = "CUP",
        isSystem = true,
    )
    val USD: CurrencyInfo = CurrencyInfo(
        id = 0L,
        name = "Dólar",
        code = "USD",
        isSystem = true,
    )
    val MLC: CurrencyInfo = CurrencyInfo(
        id = 0L,
        name = "Moneda libremente convertible",
        code = "MLC",
        isSystem = true,
    )
    val EUR: CurrencyInfo = CurrencyInfo(
        id = 0L,
        name = "Euro",
        code = "EUR",
        isSystem = true,
    )

    /** All system currencies in display order. */
    val ALL: List<CurrencyInfo> = listOf(CUP, USD, MLC, EUR)
}
