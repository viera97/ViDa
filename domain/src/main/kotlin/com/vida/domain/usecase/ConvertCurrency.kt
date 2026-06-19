package com.vida.domain.usecase

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.repository.port.CurrencyRateProvider
import java.time.Instant

/**
 * Convert [from] to [to] currency. The rate is fetched via [rateProvider].
 *
 * This is a STUB in PR #1: the port `CurrencyRateProvider` has no implementation
 * yet (it will be replaced by `CurrencyRateRepository` in PR #2a). Calling this
 * use case before PR #2a lands will throw `NoSuchElementException` because no
 * rate is available.
 */
class ConvertCurrency(private val rateProvider: CurrencyRateProvider) {
    operator fun invoke(from: Money, to: Currency, asOf: Instant = Instant.now()): Money {
        if (from.currency == to) return from
        val rate = rateProvider.getRate(from.currency, to, asOf)
            ?: throw NoSuchElementException("No rate available for ${from.currency} → $to at $asOf")
        return from.convertTo(to, rate)
    }
}