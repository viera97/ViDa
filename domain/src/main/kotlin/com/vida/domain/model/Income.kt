package com.vida.domain.model

import java.time.Instant

/**
 * A single income event — money received into a source (wallet, card, or stash).
 *
 * Invariants enforced in `init {}`:
 *
 * - `amount` MUST be positive (an "income" means money coming in; reversals are
 *   not modeled as negative incomes)
 * - `description` MUST not be blank
 * - `sourceId` MUST be non-null for CARD/STASH. For WALLET it MAY be null
 *   (legacy singleton representation) or a positive row id (current real-id
 *   wallets, commit 5742918).
 *
 * Unlike [Expense], income has no `categoryId` — incomes are not categorized the
 * same way spending is. The "what kind of income" semantic lives in
 * [description] (free text).
 *
 * Recording an income has a ledger effect on the destination source: wallet/card
 * `balance_minor` increases (via [com.vida.data.db.dao.WalletDao.adjustBalance]
 * / [com.vida.data.db.dao.CardDao.adjustBalance]); stash balance picks up the
 * delta at read time through [com.vida.data.db.dao.BalanceDao]. This is the
 * opposite of expense, which is NOT supposed to auto-update balances under
 * Option B (see [Expense] docstring); income is the explicit "auto-update"
 * path because users expect recording a salary to immediately bump their wallet
 * balance.
 *
 * @property id row id (0 means unsaved)
 * @property amount received amount in the destination source's currency
 * @property description short label, not blank (e.g. "Salario", "Regalo")
 * @property dateTime when the income happened (UTC)
 * @property sourceType which kind of source received the money
 * @property sourceId FK to the specific Wallet/Card/Stash row; null allowed only
 *                      for WALLET (legacy singleton); non-null for CARD/STASH
 *                      and for WALLET after PR #2b refactored wallets into
 *                      real entities.
 * @property note optional free-form text
 */
data class Income(
    val id: Long = 0L,
    val amount: Money,
    val description: String,
    val dateTime: Instant,
    val sourceType: SourceType,
    val sourceId: Long? = null,
    val note: String? = null,
) {
    init {
        require(amount.isPositive()) { "Income amount must be positive" }
        require(description.isNotBlank()) { "Income description must not be blank" }
        // CARD/STASH always require a non-null sourceId. WALLET may be null (legacy
        // singleton) or a positive row id (real-id wallets).
        require(sourceType == SourceType.WALLET || sourceId != null) {
            "sourceId must not be null for CARD/STASH incomes " +
                "(got sourceType=$sourceType, sourceId=$sourceId)"
        }
    }
}
