package com.vida.domain.usecase.bank

import com.vida.domain.repository.BankRepository

/**
 * Resolves a bank's color by name. Returns a fallback [0xFF607D8B] (Blue Grey)
 * when the bank is not found in the repository.
 *
 * Used by [com.vida.feature.cardmanagement.BankBranding] to resolve card gradient
 * colors from the database instead of compile-time constants.
 */
class GetBankColorByName(private val repo: BankRepository) {
    suspend operator fun invoke(name: String): Int =
        repo.getByName(name)?.color ?: 0xFF607D8B.toInt()
}
