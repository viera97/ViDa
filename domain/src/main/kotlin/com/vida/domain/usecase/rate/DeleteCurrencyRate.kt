package com.vida.domain.usecase.rate

import com.vida.domain.repository.CurrencyRateRepository

/** Deletes a currency rate by id. */
class DeleteCurrencyRate(private val repo: CurrencyRateRepository) {
    suspend operator fun invoke(id: Long) {
        require(id > 0L) { "CurrencyRate id must be > 0" }
        repo.delete(id)
    }
}