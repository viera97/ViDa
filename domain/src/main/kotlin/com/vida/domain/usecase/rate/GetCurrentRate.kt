package com.vida.domain.usecase.rate

import com.vida.domain.model.Currency
import com.vida.domain.model.CurrencyRate
import com.vida.domain.repository.CurrencyRateRepository
import java.time.Instant

/**
 * Resolves the current rate for the pair at [asOf].
 *
 * @throws NoSuchElementException when no rate has been configured for [from]→[to]
 *         on or before [asOf]. Callers must seed at least one rate per pair before
 *         attempting conversions through [com.vida.domain.usecase.ConvertCurrency].
 */
class GetCurrentRate(private val repo: CurrencyRateRepository) {
    suspend operator fun invoke(
        from: Currency,
        to: Currency,
        asOf: Instant = Instant.now(),
    ): CurrencyRate {
        require(from != to) { "from and to currencies must differ" }
        return repo.getRate(from, to, asOf)
            ?: throw NoSuchElementException("No rate configured for $from→$to at $asOf")
    }
}