package com.vida.domain.model

import java.time.LocalDate

/**
 * Template for a recurring expense. Manual generation in v1 (Q10 locked) — the
 * UI triggers [com.vida.domain.usecase.recurring.GenerateRecurringExpense] from a
 * "due today" banner or an app-launch hook; there is no WorkManager.
 *
 * Invariants enforced in `init {}`:
 *
 * - `amount` MUST be positive
 * - `description` MUST not be blank
 * - `categoryId` MUST be a positive row id
 * - `endDate` (when set) MUST be on or after `startDate`
 * - `sourceId` MUST be `null` iff `sourceType == WALLET`
 *
 * `currency` is the source's currency (mirrors the related `Expense.amount.currency`).
 *
 * @property id row id (0 means unsaved)
 * @property amount per-occurrence amount (in [currency])
 * @property currency source's currency
 * @property categoryId FK → Category.id (same category every occurrence)
 * @property sourceType which kind of source pays
 * @property sourceId FK to the specific Card/Stash row; null only when sourceType is WALLET
 * @property description short label, not blank
 * @property frequency DAILY/WEEKLY/MONTHLY/YEARLY cadence
 * @property startDate first eligible generation date
 * @property endDate optional last eligible generation date (inclusive)
 * @property lastGeneratedDate date the most recent occurrence was generated; null = never
 * @property isActive false disables generation (manual opt-out per template)
 */
data class RecurringExpense(
    val id: Long = 0L,
    val amount: Money,
    val currency: Currency,
    val categoryId: Long,
    val sourceType: SourceType,
    val sourceId: Long? = null,
    val description: String,
    val frequency: Frequency,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val lastGeneratedDate: LocalDate? = null,
    val isActive: Boolean = true,
) {
    init {
        require(amount.isPositive()) { "RecurringExpense amount must be positive" }
        require(description.isNotBlank()) { "RecurringExpense description must not be blank" }
        require(categoryId > 0L) { "RecurringExpense categoryId must be > 0" }
        require(endDate == null || !endDate.isBefore(startDate)) {
            "endDate must be on or after startDate " +
                "(got startDate=$startDate, endDate=$endDate)"
        }
        require((sourceType == SourceType.WALLET) == (sourceId == null)) {
            "sourceId must be null when sourceType is WALLET, and non-null for CARD/STASH " +
                "(got sourceType=$sourceType, sourceId=$sourceId)"
        }
    }
}
