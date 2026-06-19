package com.vida.domain.repository.port

import com.vida.domain.model.Currency
import java.math.BigDecimal
import java.time.Instant

/**
 * Port (hexagonal architecture) for fetching currency rates. Implemented in
 * :data using CurrencyRateRepository. This port exists only in PR #1; it is
 * replaced by CurrencyRateRepository in PR #2a.
 */
fun interface CurrencyRateProvider {
    fun getRate(from: Currency, to: Currency, asOf: Instant): BigDecimal?
}