package com.vida.domain.usecase.transfer

import com.vida.domain.repository.TransferRepository

/** Removes a transfer by its row id. */
class DeleteTransfer(private val repo: TransferRepository) {
    suspend operator fun invoke(id: Long) {
        require(id > 0L) { "Transfer id must be > 0" }
        repo.delete(id)
    }
}
