package com.vida.domain.usecase.bank

import com.vida.domain.model.Bank
import com.vida.domain.repository.BankRepository

/**
 * Updates an existing bank. The id MUST be > 0 (a fresh id means insert, not update).
 * Checks the bank exists before updating.
 *
 * @return the row id assigned by the persistence layer.
 */
class UpdateBank(private val repo: BankRepository) {
    suspend operator fun invoke(bank: Bank): Long {
        require(bank.id > 0L) { "Bank id must be > 0 to update" }
        val existing = repo.getById(bank.id)
            ?: throw NoSuchElementException("Bank ${bank.id} not found")
        return repo.upsert(bank)
    }
}
