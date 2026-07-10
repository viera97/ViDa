package com.vida.domain.usecase.bank

import com.vida.domain.model.Bank
import com.vida.domain.repository.BankRepository

/**
 * Persists a new user-created bank.
 *
 * @return the row id assigned by the persistence layer.
 */
class AddBank(private val repo: BankRepository) {
    suspend operator fun invoke(bank: Bank): Long = repo.upsert(bank)
}
