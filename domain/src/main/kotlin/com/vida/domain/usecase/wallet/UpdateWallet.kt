package com.vida.domain.usecase.wallet

import com.vida.domain.model.Wallet
import com.vida.domain.repository.WalletRepository

/** Upserts a wallet. [Wallet.id] must match an existing row for updates. */
class UpdateWallet(private val repo: WalletRepository) {
    suspend operator fun invoke(wallet: Wallet) {
        repo.upsert(wallet)
    }
}