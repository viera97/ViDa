package com.vida.domain.usecase.refund

import com.vida.domain.repository.RefundRepository

/** Deletes a refund by id. */
class DeleteRefund(private val repo: RefundRepository) {
    suspend operator fun invoke(id: Long) {
        require(id > 0L) { "Refund id must be > 0" }
        repo.delete(id)
    }
}