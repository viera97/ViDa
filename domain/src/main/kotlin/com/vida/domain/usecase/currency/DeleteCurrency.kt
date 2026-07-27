package com.vida.domain.usecase.currency

import com.vida.domain.repository.CurrencyRepository

/** Deletes a currency by id. */
class DeleteCurrency(private val repo: CurrencyRepository) {
    suspend operator fun invoke(id: Long) = repo.delete(id)
}
