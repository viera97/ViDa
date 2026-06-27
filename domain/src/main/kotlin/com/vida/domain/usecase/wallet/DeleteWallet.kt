package com.vida.domain.usecase.wallet

import com.vida.domain.repository.WalletRepository

/** Deletes a wallet by [id]. */
class DeleteWallet(private val repo: WalletRepository) {
    suspend operator fun invoke(id: Long) { repo.delete(id) }
}
