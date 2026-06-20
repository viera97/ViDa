package com.vida.domain.model

import java.time.Instant

/**
 * A refund tied to a single [Expense]. Modeled as a first-class entity (Q9 locked)
 * rather than as a negative expense so refunds can carry their own reason and note
 * without polluting the expense semantics.
 *
 * Invariants enforced in `init {}`:
 *
 * - `amount` MUST be positive
 * - `reason` MUST not be blank
 * - `originalExpenseId` MUST be > 0
 *
 * Uniqueness on `originalExpenseId` is enforced by the `:data` Room layer via a
 * UNIQUE index — a second refund for the same expense throws `IllegalStateException`
 * at the repo level.
 *
 * @property id row id (0 means unsaved)
 * @property originalExpenseId FK → Expense.id
 * @property amount refunded amount (in the same currency as the original expense)
 * @property reason short label, not blank (e.g., "defective product")
 * @property dateTime when the refund was recorded (UTC)
 * @property note optional free-form text
 */
data class Refund(
    val id: Long = 0L,
    val originalExpenseId: Long,
    val amount: Money,
    val reason: String,
    val dateTime: Instant,
    val note: String? = null,
) {
    init {
        require(amount.isPositive()) { "Refund amount must be positive" }
        require(reason.isNotBlank()) { "Refund reason must not be blank" }
        require(originalExpenseId > 0L) { "Refund originalExpenseId must be > 0" }
    }
}