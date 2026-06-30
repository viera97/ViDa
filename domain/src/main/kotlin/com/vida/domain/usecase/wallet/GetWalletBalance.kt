package com.vida.domain.usecase.wallet

import com.vida.domain.model.Money
import com.vida.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the balance of the wallet identified by [id].
 *
 * The returned [Flow] is reactive — Room invalidates the underlying query
 * whenever the `wallets`, `transfers`, `expenses`, or `currency_rates` tables
 * change, so consumers (typically ViewModels) automatically re-render after a
 * transfer, expense, or mutation made elsewhere in the app.
 *
 * Real implementation lives in `:data` as a Room SUM query (expenses +
 * transfers affecting the wallet), wrapped behind
 * [WalletRepository.observeBalance].
 */
class GetWalletBalance(private val repo: WalletRepository) {
    operator fun invoke(id: Long): Flow<Money> = repo.observeBalance(id)
}