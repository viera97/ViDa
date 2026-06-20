package com.vida.domain.usecase.balance

import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository
import java.time.Instant

/**
 * Returns the computed balance of a single source as of [asOf]. Dispatches to
 * the source-type-appropriate repository:
 *
 * - `WALLET` → [WalletRepository.getBalance] (singleton; `sourceId` MUST be
 *   `null` per the wallet contract, Q1 locked)
 * - `CARD` → [CardRepository.getBalance]; `sourceId` MUST be non-null
 * - `STASH` → [StashRepository.getBalance]; `sourceId` MUST be non-null
 *
 * Real implementations land in `:data` (Room SUM queries per source). The
 * three repos throw `NotImplementedError` until `:data` ships.
 */
class GetBalanceBySource(
    private val cardRepo: CardRepository,
    private val stashRepo: StashRepository,
    private val walletRepo: WalletRepository,
) {
    suspend operator fun invoke(
        sourceType: SourceType,
        sourceId: Long?,
        asOf: Instant = Instant.now(),
    ): Money = when (sourceType) {
        SourceType.WALLET -> walletRepo.getBalance(asOf)
        SourceType.CARD -> {
            require(sourceId != null) { "sourceId must be set when sourceType is CARD or STASH" }
            cardRepo.getBalance(sourceId, asOf)
        }
        SourceType.STASH -> {
            require(sourceId != null) { "sourceId must be set when sourceType is CARD or STASH" }
            stashRepo.getBalance(sourceId, asOf)
        }
    }
}
