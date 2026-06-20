package com.vida.domain.usecase

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.repository.CurrencyRateRepository
import java.time.Instant

/**
 * Convert [from] to [to] currency using the latest rate known to the
 * [CurrencyRateRepository] at [asOf] (defaults to now).
 *
 * Resolution rules:
 *
 * - **Same currency** — returns [from] unchanged; the repository is NOT consulted.
 * - **Rate available** — returns `from.convertTo(to, rate.rate)`.
 * - **No rate for the pair at [asOf]** — returns `null`. Callers that need a
 *   hard-fail semantic should use `GetCurrentRate` (which throws
 *   [NoSuchElementException]) instead. `GetTotalBalance` uses the nullable
 *   contract to drop sources whose rate is missing.
 *
 * `suspend` because [CurrencyRateRepository.getRate] is suspending.
 */
class ConvertCurrency(private val rateRepo: CurrencyRateRepository) {
    suspend operator fun invoke(from: Money, to: Currency, asOf: Instant = Instant.now()): Money? {
        if (from.currency == to) return from
        val rate = rateRepo.getRate(from.currency, to, asOf) ?: return null
        return from.convertTo(to, rate.rate)
    }
}