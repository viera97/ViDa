package com.vida.domain.usecase.wallet

import com.vida.domain.model.Wallet
import com.vida.domain.repository.WalletRepository

/**
 * Returns a wallet by [id]. Returns `null` if no wallet row exists for that id
 * — callers must upsert first.
 */
class GetWallet(private val repo: WalletRepository) {
    suspend operator fun invoke(id: Long): Wallet? = repo.getById(id)
}