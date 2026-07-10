package com.vida.domain.usecase.bank

import com.vida.domain.model.DefaultBanks
import com.vida.domain.repository.BankRepository
import kotlinx.coroutines.flow.first

/**
 * Seeds the system banks from [DefaultBanks] on first run.
 *
 * Idempotent: if any row already exists in the table the invocation is a no-op, so the
 * `:data` layer may call this from Room's `onCreate` without tracking whether it ran.
 */
class SeedDefaultBanks(private val repo: BankRepository) {
    suspend operator fun invoke() {
        val existing = repo.getAll().first()
        if (existing.isNotEmpty()) return
        DefaultBanks.ALL.forEach { bank -> repo.upsert(bank) }
    }
}
