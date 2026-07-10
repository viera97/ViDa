package com.vida.domain.usecase.bank

import com.vida.domain.model.Bank
import com.vida.domain.repository.BankRepository

/** Loads a single bank by id, or returns null if it does not exist. */
class GetBank(private val repo: BankRepository) {
    suspend operator fun invoke(id: Long): Bank? = repo.getById(id)
}
