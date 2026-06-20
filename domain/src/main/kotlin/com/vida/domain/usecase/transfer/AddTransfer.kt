package com.vida.domain.usecase.transfer

import com.vida.domain.model.Transfer
import com.vida.domain.repository.TransferRepository

/**
 * Persists a new transfer. The entity's invariants (positive amount, source/destination
 * nullness, no self-transfer) are enforced in [Transfer.init]; this use case repeats
 * the cheap ones defensively.
 *
 * Cross-entity atomicity (source balance decreases + destination balance increases
 * as one transaction) is a `:data` concern — the Room implementation of
 * [TransferRepository.upsert] wraps the insert in `withTransaction { }`.
 *
 * Cross-field business rules (none yet — `RecordTransfer` is the action use case
 * that would carry them) are deliberately omitted; this is the low-level CRUD
 * primitive for system / migration paths.
 */
class AddTransfer(private val repo: TransferRepository) {
    suspend operator fun invoke(transfer: Transfer): Long {
        require(transfer.amount.isPositive()) { "Transfer amount must be positive" }
        return repo.upsert(transfer)
    }
}
