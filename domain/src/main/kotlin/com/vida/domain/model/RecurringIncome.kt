package com.vida.domain.model

import java.time.LocalDate

/**
 * Template for a recurring income. Manual generation in v1 — the
 * UI triggers [com.vida.domain.usecase.recurring.GenerateRecurringIncome] from a
 * "due today" banner or an app-launch hook; there is no WorkManager.
 *
 * Invariants enforced in `init {}`:
 *
 * - `amount` MUST be positive
 * - `description` MUST not be blank
 * - `endDate` (when set) MUST be on or after `startDate`
 * - `sourceId` MUST be non-null for CARD/STASH; for WALLET it MAY be null (legacy
 *   singleton representation) or a positive row id (current real-id wallets).
 *
 * Note: unlike [RecurringExpense], this model has NO `categoryId` — incomes are
 * not categorized the same way spending is.
 *
 * `currency` is the source's currency (mirrors the related [Income.amount.currency]).
 *
 * @property id row id (0 means unsaved)
 * @property amount per-occurrence amount (in [currency])
 * @property currency source's currency
 * @property sourceType which kind of source receives the income
 * @property sourceId FK to the specific Wallet/Card/Stash row; null allowed only for WALLET
 *                      (legacy singleton); non-null for CARD/STASH and for WALLET after PR #2b
 *                      refactored wallets into real entities (commit 5742918).
 * @property description short label, not blank
 * @property frequency DAILY/WEEKLY/MONTHLY/YEARLY cadence
 * @property startDate first eligible generation date
 * @property endDate optional last eligible generation date (inclusive)
 * @property lastGeneratedDate date the most recent occurrence was generated; null = never
 * @property isActive false disables generation (manual opt-out per template)
 */
data class RecurringIncome(
    val id: Long = 0L,
    val amount: Money,
    val currency: String,
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
        require(amount.isPositive()) { "RecurringIncome amount must be positive" }
        require(description.isNotBlank()) { "RecurringIncome description must not be blank" }
        require(endDate == null || !endDate.isBefore(startDate)) {
            "endDate must be on or after startDate " +
                "(got startDate=$startDate, endDate=$endDate)"
        }
        // CARD/STASH always require a non-null sourceId. WALLET may be null (legacy
        // singleton representation) or a positive row id (real-id wallets).
        require(sourceType == SourceType.WALLET || sourceId != null) {
            "sourceId must not be null for CARD/STASH recurring incomes " +
                "(got sourceType=$sourceType, sourceId=$sourceId)"
        }
    }
}
