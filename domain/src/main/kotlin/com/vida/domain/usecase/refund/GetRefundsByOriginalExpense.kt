package com.vida.domain.usecase.refund

import com.vida.domain.model.Refund
import com.vida.domain.repository.RefundRepository
import kotlinx.coroutines.flow.Flow

/** Streams all refunds tied to the given original expense. */
class GetRefundsByOriginalExpense(private val repo: RefundRepository) {
    suspend operator fun invoke(expenseId: Long): Flow<List<Refund>> =
        repo.getByOriginalExpense(expenseId)
}