package com.vida.domain.model

import java.time.Instant

/**
 * A cross-source movement: money moved from one source ([fromType]/[fromId]) to
 * another ([toType]/[toId]) on [dateTime]. Transfers are first-class — they are
 * NOT modeled as `Expense` rows with an `isTransfer` flag (Q3 locked).
 *
 * Invariants enforced in `init {}`:
 *
 * - `amount` MUST be positive
 * - source and destination MUST differ — transferring to self is rejected
 *
 * All sources — wallet, card, stash — are addressed by a real row id. The
 * wallet is NOT a singleton: the multi-wallet refactor introduced an
 * `AUTOINCREMENT` PK on the `wallets` table, and a `Transfer` references that
 * row id the same way it references a card or stash row id. There are no
 * special cases anywhere in the domain.
 *
 * Cross-entity atomicity (the from-balance decreases and to-balance increases
 * as a single transaction) is a `:data` concern (Q2 locked): `TransferRepository.upsert`
 * is wrapped in `withTransaction { }` in the Room layer.
 *
 * @property id row id (0 means unsaved)
 * @property fromType which kind of source the money leaves from
 * @property fromId FK to the specific Wallet/Card/Stash row (required, non-null)
 * @property toType which kind of source the money arrives at
 * @property toId FK to the specific Wallet/Card/Stash row (required, non-null)
 * @property amount moved amount (currency is the source's currency)
 * @property dateTime when the transfer was recorded (UTC)
 * @property note optional free-form text
 */
data class Transfer(
    val id: Long = 0L,
    val fromType: SourceType,
    val fromId: Long,
    val toType: SourceType,
    val toId: Long,
    val amount: Money,
    val dateTime: Instant,
    val note: String? = null,
) {
    init {
        require(amount.isPositive()) { "Transfer amount must be positive" }
        require(fromType != toType || fromId != toId) {
            "Cannot transfer from a source to itself " +
                "(fromType=$fromType, fromId=$fromId, toType=$toType, toId=$toId)"
        }
    }
}
