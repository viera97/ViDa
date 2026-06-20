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
 * - `fromId` MUST be `null` iff `fromType == WALLET` (singleton source)
 * - `toId` MUST be `null` iff `toType == WALLET` (singleton source)
 * - source and destination MUST differ — transferring to self is rejected
 *
 * Cross-entity atomicity (the from-balance decreases and to-balance increases
 * as a single transaction) is a `:data` concern (Q2 locked): `TransferRepository.upsert`
 * is wrapped in `withTransaction { }` in the Room layer.
 *
 * @property id row id (0 means unsaved)
 * @property fromType which kind of source the money leaves from
 * @property fromId FK to the specific Card/Stash row; null only when fromType is WALLET
 * @property toType which kind of source the money arrives at
 * @property toId FK to the specific Card/Stash row; null only when toType is WALLET
 * @property amount moved amount (currency is the source's currency)
 * @property dateTime when the transfer was recorded (UTC)
 * @property note optional free-form text
 */
data class Transfer(
    val id: Long = 0L,
    val fromType: SourceType,
    val fromId: Long?,
    val toType: SourceType,
    val toId: Long?,
    val amount: Money,
    val dateTime: Instant,
    val note: String? = null,
) {
    init {
        require(amount.isPositive()) { "Transfer amount must be positive" }
        require((fromType == SourceType.WALLET) == (fromId == null)) {
            "fromId must be null when fromType is WALLET, and non-null for CARD/STASH " +
                "(got fromType=$fromType, fromId=$fromId)"
        }
        require((toType == SourceType.WALLET) == (toId == null)) {
            "toId must be null when toType is WALLET, and non-null for CARD/STASH " +
                "(got toType=$toType, toId=$toId)"
        }
        require(fromType != toType || fromId != toId) {
            "Cannot transfer from a source to itself " +
                "(fromType=$fromType, fromId=$fromId, toType=$toType, toId=$toId)"
        }
    }
}
