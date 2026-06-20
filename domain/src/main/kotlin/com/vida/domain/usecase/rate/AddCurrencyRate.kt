package com.vida.domain.usecase.rate

import com.vida.domain.model.CurrencyRate
import com.vida.domain.repository.CurrencyRateRepository

/**
 * Persists a new currency rate. Entity invariants (positive rate, distinct currencies)
 * are enforced in [CurrencyRate.init]; this use case repeats them defensively.
 */
class AddCurrencyRate(private val repo: CurrencyRateRepository) {
    suspend operator fun invoke(rate: CurrencyRate): Long {
        require(rate.rate.signum() > 0) { "CurrencyRate rate must be positive" }
        require(rate.fromCurrency != rate.toCurrency) {
            "CurrencyRate fromCurrency must differ from toCurrency"
        }
        return repo.upsert(rate)
    }
}