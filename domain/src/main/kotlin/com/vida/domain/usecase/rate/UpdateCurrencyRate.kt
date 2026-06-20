package com.vida.domain.usecase.rate

import com.vida.domain.model.CurrencyRate
import com.vida.domain.repository.CurrencyRateRepository

/** Updates an existing currency rate. The id MUST be > 0. */
class UpdateCurrencyRate(private val repo: CurrencyRateRepository) {
    suspend operator fun invoke(rate: CurrencyRate): Long {
        require(rate.id > 0L) { "CurrencyRate id must be > 0 to update" }
        return repo.upsert(rate)
    }
}