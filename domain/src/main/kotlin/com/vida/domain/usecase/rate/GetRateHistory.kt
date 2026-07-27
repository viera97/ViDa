package com.vida.domain.usecase.rate

import com.vida.domain.model.CurrencyRate
import com.vida.domain.repository.CurrencyRateRepository
import kotlinx.coroutines.flow.Flow

/** Streams every historical [CurrencyRate] for the pair, newest first. */
class GetRateHistory(private val repo: CurrencyRateRepository) {
    suspend operator fun invoke(from: String, to: String): Flow<List<CurrencyRate>> =
        repo.getRateHistory(from, to)
}