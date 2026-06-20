package com.vida.domain.usecase.transfer

import com.vida.domain.model.Transfer
import com.vida.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow

/** Reactive stream of every transfer in the system. */
class ListTransfers(private val repo: TransferRepository) {
    operator fun invoke(): Flow<List<Transfer>> = repo.getAll()
}
