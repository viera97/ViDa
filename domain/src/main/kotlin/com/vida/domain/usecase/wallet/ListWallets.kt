package com.vida.domain.usecase.wallet

import com.vida.domain.model.Wallet
import com.vida.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow

/** Returns a reactive stream of all wallets. */
class ListWallets(private val repo: WalletRepository) {
    operator fun invoke(): Flow<List<Wallet>> = repo.getAll()
}
