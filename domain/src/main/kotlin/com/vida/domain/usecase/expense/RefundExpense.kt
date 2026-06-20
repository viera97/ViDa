package com.vida.domain.usecase.expense

import com.vida.domain.model.Money
import com.vida.domain.model.Refund
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.RefundRepository
import java.time.Instant

/**
 * Records a refund tied to an existing expense. The refund is a separate
 * first-class entity (Q9 locked); it does NOT mutate the original expense.
 *
 * Validation:
 *
 * - [originalExpenseId] MUST reference a persisted expense (throws
 *   [NoSuchElementException] otherwise).
 * - [refundAmount] MUST be positive and in the same currency as the original
 *   expense (currency mismatch is an [IllegalArgumentException]).
 * - [refundAmount] MUST be `<=` the original expense's amount (the `:data`
 *   layer does not enforce this — it is a `:domain` business rule).
 * - [reason] MUST not be blank.
 *
 * Atomicity: the `:data` impl wraps the [RefundRepository.upsert] (and any
 * future source-balance credit) in Room's `withTransaction { }`. This use case
 * is a single-call delegator after validation — it MUST NOT grow business
 * logic that depends on the data layer.
 */
class RefundExpense(
    private val expenseRepo: ExpenseRepository,
    private val refundRepo: RefundRepository,
) {
    suspend operator fun invoke(
        originalExpenseId: Long,
        refundAmount: Money,
        reason: String,
        now: Instant = Instant.now(),
    ): Long {
        require(originalExpenseId > 0L) { "originalExpenseId must be > 0" }
        require(refundAmount.isPositive()) { "Refund amount must be positive" }
        require(reason.isNotBlank()) { "Refund reason must not be blank" }

        val original = expenseRepo.getById(originalExpenseId)
            ?: throw NoSuchElementException("Expense $originalExpenseId not found")

        require(refundAmount.currency == original.amount.currency) {
            "Refund currency (${refundAmount.currency}) must match original expense currency (${original.amount.currency})"
        }
        require(refundAmount <= original.amount) {
            "Refund amount ($refundAmount) must be ≤ original expense amount (${original.amount})"
        }

        val refund = Refund(
            id = 0L,
            originalExpenseId = originalExpenseId,
            amount = refundAmount,
            reason = reason,
            dateTime = now,
        )
        return refundRepo.upsert(refund)
    }
}
