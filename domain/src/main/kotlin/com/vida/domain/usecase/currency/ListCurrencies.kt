package com.vida.domain.usecase.currency

import com.vida.domain.model.CurrencyInfo
import com.vida.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow

/** Streams the full currency list. Reactive: emits on every Room table change. */
class ListCurrencies(private val repo: CurrencyRepository) {
    operator fun invoke(): Flow<List<CurrencyInfo>> = repo.getAll()
}
