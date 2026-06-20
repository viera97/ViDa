package com.vida.domain.usecase.transfer

import com.vida.domain.model.Transfer
import com.vida.domain.repository.TransferRepository

/**
 * Action use case for recording a new transfer. This is the path the UI uses
 * when the user taps "Confirm" on the transfer dialog.
 *
 * Validation responsibilities:
 *
 * - **Entity-level invariants** (positive amount, source/destination nullness
 *   rules, no self-transfer) are enforced by [Transfer.init] before the use
 *   case body runs. This use case repeats the cheap checks defensively so the
 *   failure messages stay near the call site if the entity contract is ever
 *   loosened.
 * - **Cross-entity atomicity** — that the source's balance decreases and the
 *   destination's balance increases as one transaction — is **NOT** handled
 *   here. It is a `:data` concern: [TransferRepository.upsert] is wrapped in
 *   Room's `withTransaction { }` so the single-statement insert is atomic on
 *   its own. If the future introduces a stored `Balance` column, the `:data`
 *   impl upgrades to a multi-statement transaction without changing this use
 *   case's contract.
 *
 * This class is intentionally a thin validator + single-call delegator — it
 * MUST NOT grow business logic that depends on the `:data` layer.
 */
class RecordTransfer(private val repo: TransferRepository) {
    suspend operator fun invoke(transfer: Transfer): Long {
        require(transfer.amount.isPositive()) { "Transfer amount must be positive" }
        require(transfer.fromType != transfer.toType || transfer.fromId != transfer.toId) {
            "Cannot transfer from a source to itself " +
                "(fromType=${transfer.fromType}, fromId=${transfer.fromId}, " +
                "toType=${transfer.toType}, toId=${transfer.toId})"
        }
        return repo.upsert(transfer)
    }
}
