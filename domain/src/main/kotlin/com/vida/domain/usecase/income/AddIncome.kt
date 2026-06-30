package com.vida.domain.usecase.income

import com.vida.domain.model.Income
import com.vida.domain.repository.IncomeRepository

/**
 * Persists a new income. The entity's invariants (positive amount, non-blank
 * description, source/sourceId pairing) are enforced in [Income.init]; this use
 * case repeats the cheap ones defensively.
 *
 * Cross-entity validation (destination source exists) lives in [RecordIncome]
 * — this is the low-level CRUD primitive.
 */
class AddIncome(private val repo: IncomeRepository) {
    suspend operator fun invoke(income: Income): Long {
        require(income.amount.isPositive()) { "Income amount must be positive" }
        require(income.description.isNotBlank()) { "Income description must not be blank" }
        return repo.upsert(income)
    }
}
