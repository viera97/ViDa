package com.vida.domain.usecase.currency

import com.vida.domain.model.DefaultCurrencies
import com.vida.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.first

/**
 * Seeds the system currencies from [DefaultCurrencies] on first run.
 *
 * Idempotent: if any row already exists in the table the invocation is a no-op, so the
 * `:data` layer may call this from Room's `onCreate` without tracking whether it ran.
 */
class SeedDefaultCurrencies(private val repo: CurrencyRepository) {
    suspend operator fun invoke() {
        val existing = repo.getAll().first()
        if (existing.isNotEmpty()) return
        DefaultCurrencies.ALL.forEach { currency -> repo.upsert(currency) }
    }
}
