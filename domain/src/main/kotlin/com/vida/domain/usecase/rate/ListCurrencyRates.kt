package com.vida.domain.usecase.rate

import com.vida.domain.model.CurrencyRate
import com.vida.domain.repository.CurrencyRateRepository
import kotlinx.coroutines.flow.Flow

/** Streams the full currency rate table. */
class ListCurrencyRates(private val repo: CurrencyRateRepository) {
    operator fun invoke(): Flow<List<CurrencyRate>> = repo.getAll()
}