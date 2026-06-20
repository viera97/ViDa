package com.vida.domain.model

import java.time.Instant

/**
 * A single spending event.
 *
 * Invariants enforced in `init {}`:
 *
 * - `amount` MUST be positive (a refund is modeled as a separate [Refund], not a negative expense)
 * - `description` MUST not be blank
 * - `categoryId` MUST be a positive row id (room auto-assigns > 0)
 * - `realAmount`, when present, MUST be in the same currency as `amount` (Q4 locked)
 * - `sourceId` MUST be `null` iff `sourceType == WALLET`; non-null for CARD/STASH (Q-PR1-3)
 *
 * Transfers are NOT modeled as expenses with an `isTransfer` flag (Q3 dropped);
 * they live in PR #2b as a first-class `Transfer` entity.
 *
 * @property id row id (0 means unsaved)
 * @property categoryId FK → Category.id
 * @property amount declared amount in the source's currency
 * @property realAmount actual amount paid (same currency as [amount]); used when the receipt
 *                      shows a different number than the planned amount (e.g., tips, rounding)
 * @property description short label, not blank
 * @property dateTime when the expense happened (UTC)
 * @property sourceType which kind of source paid for this
 * @property sourceId FK to the specific Card/Stash row; null only when sourceType is WALLET
 * @property note optional free-form text
 */
data class Expense(
    val id: Long = 0L,
    val categoryId: Long,
    val amount: Money,
    val realAmount: Money? = null,
    val description: String,
    val dateTime: Instant,
    val sourceType: SourceType,
    val sourceId: Long? = null,
    val note: String? = null,
) {
    init {
        require(amount.isPositive()) { "Expense amount must be positive" }
        require(description.isNotBlank()) { "Expense description must not be blank" }
        require(categoryId > 0L) { "Expense categoryId must be > 0" }
        require(realAmount == null || realAmount.currency == amount.currency) {
            "Expense realAmount currency (${realAmount?.currency}) must match amount currency (${amount.currency})"
        }
        require((sourceType == SourceType.WALLET) == (sourceId == null)) {
            "sourceId must be null when sourceType is WALLET, and non-null for CARD/STASH " +
                "(got sourceType=$sourceType, sourceId=$sourceId)"
        }
    }
}