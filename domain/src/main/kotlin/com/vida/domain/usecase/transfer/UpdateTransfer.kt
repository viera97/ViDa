package com.vida.domain.usecase.transfer

import com.vida.domain.model.Transfer
import com.vida.domain.repository.TransferRepository

/** Updates an existing transfer. The id MUST be > 0 — use [AddTransfer] for new transfers. */
class UpdateTransfer(private val repo: TransferRepository) {
    suspend operator fun invoke(transfer: Transfer): Long {
        require(transfer.id > 0L) { "Transfer id must be > 0 to update (use AddTransfer for new transfers)" }
        return repo.upsert(transfer)
    }
}
