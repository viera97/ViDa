package com.vida.domain.repository

import com.vida.domain.model.Money
import com.vida.domain.model.Wallet
import java.time.Instant

/**
 * Persistence contract for the singleton [Wallet]. Implemented in `:data` (Room).
 *
 * [get] returns the only wallet row; it throws (typically `NoSuchElementException`) if
 * no wallet has been seeded yet — callers must invoke [upsert] first.
 */
interface WalletRepository {
    suspend fun get(): Wallet
    suspend fun upsert(wallet: Wallet)
    suspend fun getBalance(asOf: Instant = Instant.now()): Money
}