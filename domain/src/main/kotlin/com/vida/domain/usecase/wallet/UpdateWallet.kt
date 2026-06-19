package com.vida.domain.usecase.wallet

import com.vida.domain.model.Wallet
import com.vida.domain.repository.WalletRepository

/** Updates the singleton wallet. The [Wallet.id] must equal `1L`. */
class UpdateWallet(private val repo: WalletRepository) {
    suspend operator fun invoke(wallet: Wallet) {
        require(wallet.id == 1L) { "Wallet is a singleton; id must be 1" }
        repo.upsert(wallet)
    }
}