package com.vida.domain.usecase.currency

import com.vida.domain.model.CurrencyInfo
import com.vida.domain.repository.CurrencyRepository

/**
 * Persists a new user-created currency.
 *
 * @return the row id assigned by the persistence layer.
 */
class AddCurrency(private val repo: CurrencyRepository) {
    suspend operator fun invoke(currency: CurrencyInfo): Long = repo.upsert(currency)
}
