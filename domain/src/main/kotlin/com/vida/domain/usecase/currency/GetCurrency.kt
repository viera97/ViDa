package com.vida.domain.usecase.currency

import com.vida.domain.model.CurrencyInfo
import com.vida.domain.repository.CurrencyRepository

/** Loads a single currency by id, or returns null if it does not exist. */
class GetCurrency(private val repo: CurrencyRepository) {
    suspend operator fun invoke(id: Long): CurrencyInfo? = repo.getById(id)
}
