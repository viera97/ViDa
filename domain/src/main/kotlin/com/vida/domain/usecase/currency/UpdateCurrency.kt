package com.vida.domain.usecase.currency

import com.vida.domain.model.CurrencyInfo
import com.vida.domain.repository.CurrencyRepository

/**
 * Updates an existing currency. The id MUST be > 0 (a fresh id means insert, not update).
 * Checks the currency exists before updating.
 *
 * @return the row id assigned by the persistence layer.
 */
class UpdateCurrency(private val repo: CurrencyRepository) {
    suspend operator fun invoke(currency: CurrencyInfo): Long {
        require(currency.id > 0L) { "Currency id must be > 0 to update" }
        val existing = repo.getById(currency.id)
            ?: throw NoSuchElementException("Currency ${currency.id} not found")
        return repo.upsert(currency)
    }
}
