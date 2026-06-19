package com.vida.domain.usecase.wallet

import com.vida.domain.model.Wallet
import com.vida.domain.repository.WalletRepository

/**
 * Returns the singleton wallet. Throws (via the repo's `:data` impl) if no wallet
 * row has been seeded yet — callers must upsert first.
 */
class GetWallet(private val repo: WalletRepository) {
    suspend operator fun invoke(): Wallet = repo.get()
}