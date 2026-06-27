package com.vida.domain.repository

import com.vida.domain.model.Money
import com.vida.domain.model.Wallet
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for [Wallet] aggregates. Implemented in `:data` (Room).
 *
 * The reactive [getAll] and [observeBalance] emit on every underlying table change;
 * consumers (UI ViewModels via [com.vida.domain.usecase.wallet.ListWallets] and
 * [com.vida.domain.usecase.wallet.GetWalletBalance]) collect them as state so the UI
 * stays in sync after transfers, expenses, or mutations made elsewhere in the app.
 */
interface WalletRepository {
    fun getAll(): Flow<List<Wallet>>
    suspend fun getById(id: Long): Wallet?
    suspend fun upsert(wallet: Wallet)
    suspend fun delete(id: Long)
    fun observeBalance(id: Long): Flow<Money>
}