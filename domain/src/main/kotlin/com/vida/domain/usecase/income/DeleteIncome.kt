package com.vida.domain.usecase.income

import com.vida.domain.repository.IncomeRepository

/** Deletes an income by id. */
class DeleteIncome(private val repo: IncomeRepository) {
    suspend operator fun invoke(id: Long) {
        require(id > 0L) { "Income id must be > 0" }
        repo.delete(id)
    }
}
