package com.vida.domain.model

/**
 * Currency available for wallet association.
 *
 * - **System currencies** — seeded by [DefaultCurrencies]; `isSystem = true`. Their
 *   deletion is blocked by the UI.
 * - **User currencies** — created via `AddCurrency`; `isSystem = false`. Fully editable.
 *
 * @property id row id (0 means unsaved)
 * @property name display name (1..50 chars, not blank)
 * @property code ISO 4217-like abbreviation (1..10 chars, not blank, unique)
 * @property isSystem true iff this row came from [DefaultCurrencies]
 */
data class CurrencyInfo(
    val id: Long = 0L,
    val name: String,
    val code: String,
    val isSystem: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Currency name must not be blank" }
        require(name.length <= 50) { "Currency name must be ≤ 50 chars" }
        require(code.isNotBlank()) { "Currency code must not be blank" }
        require(code.length <= 10) { "Currency code must be ≤ 10 chars" }
    }
}
