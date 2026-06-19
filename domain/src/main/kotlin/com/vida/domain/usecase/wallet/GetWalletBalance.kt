package com.vida.domain.usecase.wallet

import com.vida.domain.model.Money
import com.vida.domain.repository.WalletRepository
import java.time.Instant

/**
 * Computes the wallet balance. Real implementation lives in :data as a Room
 * SUM query (expenses + transfers affecting the wallet). The repo throws
 * NotImplementedError in PR #1 because no Room impl exists yet.
 */
class GetWalletBalance(private val repo: WalletRepository) {
    suspend operator fun invoke(asOf: Instant = Instant.now()): Money = repo.getBalance(asOf)
}