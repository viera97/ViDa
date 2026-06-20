package com.vida.domain.usecase.transfer

import com.vida.domain.model.Transfer
import com.vida.domain.repository.TransferRepository

/** Loads a single transfer by id; returns null if not found. */
class GetTransfer(private val repo: TransferRepository) {
    suspend operator fun invoke(id: Long): Transfer? {
        require(id > 0L) { "Transfer id must be > 0" }
        return repo.getById(id)
    }
}
