package com.vida.domain.usecase.bank

import com.vida.domain.repository.BankRepository

/** Deletes a bank by id. */
class DeleteBank(private val repo: BankRepository) {
    suspend operator fun invoke(id: Long) = repo.delete(id)
}
