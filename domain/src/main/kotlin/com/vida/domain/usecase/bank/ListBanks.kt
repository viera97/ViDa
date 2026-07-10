package com.vida.domain.usecase.bank

import com.vida.domain.model.Bank
import com.vida.domain.repository.BankRepository
import kotlinx.coroutines.flow.Flow

/** Streams the full bank list. Reactive: emits on every Room table change. */
class ListBanks(private val repo: BankRepository) {
    operator fun invoke(): Flow<List<Bank>> = repo.getAll()
}
