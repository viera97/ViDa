package com.vida.domain.usecase.refund

import com.vida.domain.model.Refund
import com.vida.domain.repository.RefundRepository

/** Updates an existing refund. The id MUST be > 0. */
class UpdateRefund(private val repo: RefundRepository) {
    suspend operator fun invoke(refund: Refund): Long {
        require(refund.id > 0L) { "Refund id must be > 0 to update" }
        return repo.upsert(refund)
    }
}