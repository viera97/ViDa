package com.vida.domain.usecase.income

import com.vida.domain.model.Income
import com.vida.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow

/** Streams the full income list. Reactive: emits on every Room table change. */
class ListIncomes(private val repo: IncomeRepository) {
    operator fun invoke(): Flow<List<Income>> = repo.getAll()
}
