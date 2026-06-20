package com.vida.domain.usecase.refund

import com.vida.domain.model.Refund
import com.vida.domain.repository.RefundRepository

/**
 * Persists a new refund. The entity's invariants (positive amount, non-blank reason)
 * are enforced in [Refund.init]; this use case repeats them defensively.
 *
 * Cross-field validation against the original expense (currency match, refund ≤ expense)
 * lives in PR #2b's `RefundExpense` which has the original [com.vida.domain.model.Expense]
 * in scope. `AddRefund` is the low-level CRUD primitive.
 */
class AddRefund(private val repo: RefundRepository) {
    suspend operator fun invoke(refund: Refund): Long {
        require(refund.amount.isPositive()) { "Refund amount must be positive" }
        require(refund.reason.isNotBlank()) { "Refund reason must not be blank" }
        return repo.upsert(refund)
    }
}